package all_class;

import java.io.*;
import java.nio.file.*;
import java.util.*;

// 2.0：用对象流把对象直接写入二进制文件，不需要逐个拼接字符串。
class BinaryStore {
    private final Path directory;
    private final Path file;
    private final Path backup;
    private boolean loaded;
    private boolean saved;
    private boolean usingBackup;
    private boolean importedSavedData;

    // Serializable 表示这种对象可以被 ObjectOutputStream 保存。
    static class State implements Serializable {
        private static final long serialVersionUID = 1L;
        List<Administrators> admins = new ArrayList<>();
        List<Customer> customers = new ArrayList<>();
        List<Goods> goods = new ArrayList<>();
        int lastOrderId = 1000;
    }

    BinaryStore(Path directory) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory);
        file = directory.resolve("supermarket-data.dat");
        backup = directory.resolve("supermarket-data.dat.bak");
    }

    State load() throws IOException {
        loaded = false;
        State state;
        if (Files.exists(file) || Files.exists(backup)) {
            try {
                state = read(file);
                usingBackup = false;
            } catch (IOException e) {
                state = read(backup);
                usingBackup = true;
                System.out.println("二进制文件读取失败，已使用上一份备份，最近一次修改可能丢失。");
            }
            saved = true;

        } else {
            // 只有首次运行 2.0 才导入 1.0 的 TXT，旧文件不修改。
            TxtStore oldStore = new TxtStore(directory);
            TxtStore.State old = oldStore.load();
            importedSavedData = oldStore.hasSavedData();
            state = new State();
            state.admins = old.admins;
            state.customers = old.customers;
            state.goods = old.goods;
            state.lastOrderId = old.lastOrderId;
        }
        loaded = true;
        return state;
    }

    private State read(Path path) throws IOException {
        try (InputStream fileInput = Files.newInputStream(path);
             ObjectInputStream input = new ObjectInputStream(fileInput)) {
            if (!input.readUTF().equals("SUPERMARKET-2.0")) throw new IOException("不是 2.0 数据文件");
            State state = (State) input.readObject();
            if (!input.readUTF().equals("END")) throw new IOException("文件未写完整");
            if (state == null || state.admins == null || state.customers == null || state.goods == null
                    || state.admins.isEmpty()) throw new IOException("数据不完整");
            return state;
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IOException("无法读取文件中的对象", e);
        }
    }

    void save(List<Administrators> admins, List<Customer> customers, List<Goods> goods, int lastOrderId) throws IOException {
        if (!loaded) throw new IOException("读取失败，不能覆盖原数据");
        State state = new State();
        state.admins = admins;
        state.customers = customers;
        state.goods = goods;
        state.lastOrderId = lastOrderId;

        Path temp = directory.resolve("supermarket-data.dat.tmp");
        try (OutputStream fileOutput = Files.newOutputStream(temp);
             ObjectOutputStream output = new ObjectOutputStream(fileOutput)) {
            output.writeUTF("SUPERMARKET-2.0");
            // 三个列表放在同一个对象中保存，购物车和订单也会随顾客对象一起保存。
            output.writeObject(state);
            output.writeUTF("END");
        }
        if (Files.exists(file) && !usingBackup) {
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        usingBackup = false;
        saved = true;
    }

    boolean hasSavedData() { return saved; }
    boolean shouldCreateDefaults() { return !saved && !importedSavedData; }
}
