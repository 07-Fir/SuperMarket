package all_class;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

// 只负责 TXT 的读取和保存
class TxtStore {
    private final Path directory;
    private final Path file;
    private final Path backup;
    private boolean loaded = false;
    private boolean saved = false;
    private boolean usingBackup = false;

    // 把读取到的三种对象一起交给主程序。
    static class State {
        List<Administrators> admins = new ArrayList<>();
        List<Customer> customers = new ArrayList<>();
        List<Goods> goods = new ArrayList<>();
        int lastOrderId = 1000;
    }

    TxtStore(Path directory) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory);
        file = directory.resolve("supermarket-data.txt");
        backup = directory.resolve("supermarket-data.txt.bak");
    }

    State load() throws IOException {
        loaded = false;
        State state;
        if (Files.exists(file) || Files.exists(backup)) {
            try {
                state = readData(file);
                usingBackup = false;
            } catch (IOException e) {
                // 正式文件打不开或内容不完整时，尝试上一份备份。
                state = readData(backup);
                usingBackup = true;
                System.out.println("正式文件读取失败，已使用上一份备份，最近一次修改可能丢失。");
            }
            saved = true;
        } else {
            state = new State();
            // 第一次使用新版时，读入原来的三个文件，不修改原文件。
            readOldFile("admin.txt", "A", state);
            readOldFile("goods.txt", "G", state);
            readOldFile("customers.txt", "C", state);
            if (Files.exists(directory.resolve("admin.txt")) && state.admins.isEmpty())
                throw new IOException("admin.txt 为空，请先检查原数据");
            if (Files.exists(directory.resolve("goods.txt")) && state.goods.isEmpty())
                throw new IOException("goods.txt 为空，请先检查原数据");
        }
        loaded = true;
        return state;
    }

    private void readOldFile(String name, String type, State state) throws IOException {
        Path oldFile = directory.resolve(name);
        if (!Files.exists(oldFile)) return;
        List<String> lines = Files.readAllLines(oldFile, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (i == 0 && line.startsWith("\uFEFF")) line = line.substring(1);
            if (!line.isBlank()) readRecord(type + "\t" + line, state, name, i + 1);
        }
    }

    private State readData(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.size() < 3 || !lines.get(0).equals("SUPERMARKET-TXT-2"))
            throw new IOException(path.getFileName() + " 内容不完整");
        String lastLine = lines.get(lines.size() - 1);
        // END 表示文件已写完；同时兼容上一版的结尾，不再计算摘要。
        if (!lastLine.equals("END") && !lastLine.matches("SHA256\t[0-9a-f]{64}"))
            throw new IOException(path.getFileName() + " 缺少结束标记");
        State state = new State();
        for (int i = 1; i < lines.size() - 1; i++) {
            readRecord(lines.get(i), state, path.getFileName().toString(), i + 1);
        }
        if (state.admins.isEmpty()) throw new IOException("管理员数据缺失");
        return state;
    }

    // 每行开头说明对象类型：A 管理员、G 商品、C 顾客、O 订单、K 购物车。
    private void readRecord(String line, State state, String name, int number) throws IOException {
        try {
            String[] record = line.split("\t", 2);
            String data = record[1];
            switch (record[0]) {
                case "A":
                    Administrators admin = Administrators.fromFileLine(data);
                    if (admin == null) throw new IllegalArgumentException();
                    state.admins.add(admin);
                    break;
                case "G":
                    Goods goods = Goods.fromFileLine(data);
                    if (goods == null) throw new IllegalArgumentException();
                    state.goods.add(goods);
                    break;
                case "C":
                    Customer customer = Customer.fromFileLine(data);
                    if (customer == null) throw new IllegalArgumentException();
                    state.customers.add(customer);
                    break;
                case "O":
                    Map<String, Customer> customers = new HashMap<>();
                    for (Customer c : state.customers) customers.put(c.getUserID(), c);
                    Order order = Order.fromFileLine(data, customers);
                    order.getCustomer().addOrder(order);
                    state.lastOrderId = Math.max(state.lastOrderId, order.getOrderID());
                    break;
                case "K":
                    String[] fields = TextCodec.split(data);
                    if (fields.length != 3) throw new IllegalArgumentException();
                    Customer owner = null;
                    Goods item = null;
                    for (Customer c : state.customers) {
                        if (c.getUserID().equals(fields[0])) owner = c;
                    }
                    for (Goods g : state.goods) {
                        if (g.getGoodsID().equals(fields[1])) item = g;
                    }
                    int count = Integer.parseInt(fields[2]);
                    if (owner == null || item == null || count <= 0) throw new IllegalArgumentException();
                    owner.getCart().put(item, count);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        } catch (RuntimeException e) {
            throw new IOException(name + " 第 " + number + " 行格式错误，停止读取", e);
        }
    }

    void save(List<Administrators> admins, List<Customer> customers, List<Goods> goods) throws IOException {
        if (!loaded) throw new IOException("读取失败，不能覆盖原数据");
        List<String> lines = new ArrayList<>();
        lines.add("SUPERMARKET-TXT-2");
        for (Administrators a : admins) lines.add("A\t" + a.toFileLine());
        for (Goods g : goods) lines.add("G\t" + g.toFileLine());
        for (Customer c : customers) lines.add("C\t" + c.toFileLine());
        for (Customer c : customers) {
            for (Order order : c.getOrderHistory()) lines.add("O\t" + order.toFileLine());
            for (Map.Entry<Goods, Integer> entry : c.getCart().entrySet()) {
                lines.add("K\t" + TextCodec.join(c.getUserID(), entry.getKey().getGoodsID(), entry.getValue()));
            }
        }
        lines.add("END");


        Path temp = directory.resolve("supermarket-data.txt.tmp");
        Files.write(temp, lines, StandardCharsets.UTF_8);
        // 如果正在使用备份，就不能用损坏的正式文件覆盖这份备份。
        if (Files.exists(file) && !usingBackup) {
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
        }
        // 保留这一行原子替换，避免停止程序时把原文件清空。
        Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        usingBackup = false;
        saved = true;
    }

    boolean hasSavedData() { return saved; }
}
