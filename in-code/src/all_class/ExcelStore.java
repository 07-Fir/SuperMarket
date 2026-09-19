package all_class;

import java.io.IOException;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;

/** 3.0：一个 xlsx 文件保存全部业务数据，不再写入 TXT 或 DAT。 */
final class ExcelStore implements AutoCloseable {
    private final Path directory, file, backup;
    private final FileChannel lockChannel;
    private final FileLock lock;
    private boolean loaded, saved, usingBackup, createDefaults;
    private static final String FORMAT = "SUPERMARKET-3.0-PBKDF2";
    private static final Map<String, List<Object>> HEADERS;
    static {
        Map<String, List<Object>> headers = new LinkedHashMap<>();
        headers.put("管理员", List.of("账号", "密码哈希", "是否默认密码"));
        headers.put("用户", List.of("用户ID", "用户名", "密码哈希", "手机号", "邮箱", "注册时间(北京时间)", "累计消费", "连续登录失败次数", "是否锁定"));
        headers.put("商品", List.of("商品ID", "商品名称", "厂家", "生产时间(北京时间)", "型号", "进价", "售价", "库存"));
        headers.put("购物车", List.of("用户ID", "商品ID", "数量"));
        headers.put("订单", List.of("订单号", "用户ID", "支付方式", "购买时间(北京时间)", "总额"));
        headers.put("订单明细", List.of("订单号", "商品ID", "商品名称", "厂家", "生产时间(北京时间)", "型号", "购买时进价", "购买时售价", "购买时库存", "购买数量"));
        headers.put("系统信息", List.of("数据格式", "最后订单号"));
        HEADERS = Collections.unmodifiableMap(headers);
    }

    static final class State {
        List<Administrators> admins = new ArrayList<>();
        List<Customer> customers = new ArrayList<>();
        List<Goods> goods = new ArrayList<>();
        int lastOrderId = 1000;
        boolean legacyPasswords;
    }

    ExcelStore(Path directory) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory);
        file = directory.resolve("supermarket-data.xlsx");
        backup = directory.resolve("supermarket-data.xlsx.bak");
        lockChannel = FileChannel.open(directory.resolve("supermarket-data.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock acquired;
        try {
            acquired = lockChannel.tryLock();
            if (acquired == null) throw new IOException("另一个程序正在使用此数据目录，请先关闭它");
        } catch (IOException | OverlappingFileLockException e) {
            lockChannel.close();
            throw new IOException("无法独占数据目录，请检查是否已启动另一个程序", e);
        }
        lock = acquired;
    }

    State load() throws IOException {
        loaded = false;
        State state;
        if (Files.exists(file) || Files.exists(backup)) {
            try {
                state = read(file);
                usingBackup = false;
            } catch (IOException mainError) {
                try { state = read(backup); }
                catch (IOException backupError) {
                    mainError.addSuppressed(backupError);
                    throw new IOException("Excel 及备份均无法读取，已禁止覆盖保存：" + mainError.getMessage(), mainError);
                }
                usingBackup = true;
                System.out.println("Excel 文件读取失败，已恢复上一份备份，最近一次修改可能丢失。");
            }
            saved = true;
            createDefaults = false;
        } else {
            // BinaryStore 会继续兼容 1.0 的 TXT；这里只读取旧文件，不覆盖。
            BinaryStore oldStore = new BinaryStore(directory);
            BinaryStore.State old = oldStore.load();
            state = new State();
            state.admins = old.admins;
            state.customers = old.customers;
            state.goods = old.goods;
            state.lastOrderId = old.lastOrderId;
            createDefaults = oldStore.shouldCreateDefaults();
            saved = false;
        }
        loaded = true;
        if (saved && (state.legacyPasswords || backupNeedsUpgrade())) {
            save(state.admins, state.customers, state.goods, state.lastOrderId);
            System.out.println("Excel 密码及备份已升级为加盐哈希，原密码仍可正常登录。");
        }
        return state;
    }

    private boolean backupNeedsUpgrade() {
        if (!Files.exists(backup)) return false;
        try { return read(backup).legacyPasswords; }
        catch (IOException e) { return true; } // 用已验证的正式数据修复损坏备份。
    }

    void save(List<Administrators> admins, List<Customer> customers, List<Goods> goods, int lastOrderId) throws IOException {
        if (!loaded) throw new IOException("未成功读取数据，不能覆盖保存");
        State state = new State();
        state.admins = admins;
        state.customers = customers;
        state.goods = goods;
        state.lastOrderId = lastOrderId;
        Path temp = Files.createTempFile(directory, "supermarket-data-", ".tmp");
        try {
            writeValidated(temp, state);
            Path previous = usingBackup ? backup : file;
            if (Files.exists(previous)) {
                // 重新序列化已验证的旧状态，不能直接复制含明文密码的旧工作簿。
                State previousState = read(previous);
                Path backupTemp = Files.createTempFile(directory, "supermarket-data-backup-", ".tmp");
                try {
                    writeValidated(backupTemp, previousState);
                    Files.move(backupTemp, backup, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } finally { Files.deleteIfExists(backupTemp); }
            }
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            usingBackup = false;
            saved = true;
        } finally { Files.deleteIfExists(temp); }
    }

    private void writeValidated(Path path, State state) throws IOException {
        Map<String, List<List<Object>>> sheets = new LinkedHashMap<>();
        HEADERS.forEach((name, header) -> sheets.put(name, new ArrayList<>(List.of(header))));
        for (Administrators a : state.admins) add(sheets, "管理员", a.getAccount(), a.getPasswordHash(), a.isDefaultPassword());
        for (Goods g : state.goods) sheets.get("商品").add(goodsRow(g));
        for (Customer c : state.customers) {
            add(sheets, "用户", c.getUserID(), c.getUserName(), c.getPasswordHash(), c.getPhone(), c.getEmail(),
                    c.getRegisterTime(), c.getTotalConsumption(), c.getLoginTimes(), c.isLocked());
            for (Map.Entry<Goods, Integer> entry : c.getCart().entrySet())
                add(sheets, "购物车", c.getUserID(), entry.getKey().getGoodsID(), entry.getValue());
            for (Order order : c.getOrderHistory()) {
                add(sheets, "订单", order.getOrderID(), c.getUserID(), order.getPaymentMethod(), order.getBuyTime(), order.getTotalAmount());
                for (Goods g : order.getGoods()) {
                    Integer count = order.getGoodsCountMap().get(g.getGoodsID());
                    // 兼容旧版订单可能包含未购买商品的快照，只保存实际购买项。
                    if (count == null || count == 0) continue;
                    List<Object> row = new ArrayList<>();
                    row.add(order.getOrderID());
                    row.addAll(goodsRow(g));
                    row.add(count);
                    sheets.get("订单明细").add(row);
                }
            }
        }
        add(sheets, "系统信息", FORMAT, state.lastOrderId);
        XlsxFile.write(path, sheets);
        read(path); // 完整校验关联及金额后，才替换正式文件。
    }

    // 供 4.0 导入使用，只读取且不修改旧 Excel，由调用方持有数据目录锁。
    static State readForMigration(Path path) throws IOException { return read(path); }

    private static State read(Path path) throws IOException {
        Map<String, List<List<String>>> sheets = XlsxFile.read(path);
        try {
            List<List<String>> metadata = sheets.get("系统信息");
            require(metadata != null && metadata.size() == 2 && metadata.get(1).size() == 2, "系统信息缺失");
            String format = metadata.get(1).get(0);
            boolean legacy = format.equals("SUPERMARKET-3.0");
            require(legacy || format.equals(FORMAT), "数据格式版本错误");
            for (Map.Entry<String, List<Object>> header : HEADERS.entrySet()) {
                List<List<String>> rows = sheets.get(header.getKey());
                List<Object> expected = new ArrayList<>(header.getValue());
                if (legacy) Collections.replaceAll(expected, "密码哈希", "密码");
                require(rows != null && !rows.isEmpty() && rows.get(0).equals(expected), "Sheet 或表头错误：" + header.getKey());
                for (List<String> row : rows) {
                    // Excel 再保存时可能省略末尾空单元格。
                    while (row.size() < header.getValue().size()) row.add("");
                    require(row.size() == header.getValue().size(), "列数错误：" + header.getKey());
                }
            }
            List<List<String>> meta = rows(sheets, "系统信息");
            int lastId = integer(meta.get(0).get(1));
            require(lastId >= 1000, "最后订单号无效");
            State state = new State();
            state.lastOrderId = lastId;
            state.legacyPasswords = legacy;
            Set<String> accounts = new HashSet<>();
            for (List<String> row : rows(sheets, "管理员")) {
                require(!row.get(0).isBlank() && accounts.add(row.get(0)), "管理员账号为空或重复");
                Administrators a = legacy ? new Administrators(row.get(0), row.get(1))
                        : Administrators.fromPasswordHash(row.get(0), row.get(1));
                a.setDefaultPassword(bool(row.get(2)));
                state.admins.add(a);
            }
            require(!state.admins.isEmpty(), "管理员数据缺失");
            Map<String, Customer> customers = new LinkedHashMap<>();
            for (List<String> row : rows(sheets, "用户")) {
                Customer c = legacy ? new Customer(row.get(1), row.get(2), row.get(3), row.get(4))
                        : Customer.fromPasswordHash(row.get(1), row.get(2), row.get(3), row.get(4));
                c.setUserID(row.get(0));
                c.setRegisterTime(XlsxFile.date(row.get(5)));
                c.setTotalConsumption(amount(row.get(6)));
                c.setLoginTimes(integer(row.get(7)));
                c.setLocked(bool(row.get(8)));
                require(!c.getUserID().isBlank() && customers.put(c.getUserID(), c) == null, "用户ID为空或重复");
            }
            state.customers.addAll(customers.values());
            Map<String, Goods> goods = new LinkedHashMap<>();
            for (List<String> row : rows(sheets, "商品")) {
                Goods g = goods(row);
                require(goods.put(g.getGoodsID(), g) == null, "商品ID重复");
            }
            state.goods.addAll(goods.values());
            for (List<String> row : rows(sheets, "购物车")) {
                Customer c = customers.get(row.get(0));
                Goods g = goods.get(row.get(1));
                int count = integer(row.get(2));
                require(c != null && g != null && count > 0, "购物车引用或数量无效");
                require(c.getCart().put(g, count) == null, "购物车商品重复");
            }
            Map<Integer, List<List<String>>> details = new HashMap<>();
            for (List<String> row : rows(sheets, "订单明细"))
                details.computeIfAbsent(integer(row.get(0)), key -> new ArrayList<>()).add(row);
            Set<Integer> orderIds = new HashSet<>();
            for (List<String> row : rows(sheets, "订单")) {
                int id = integer(row.get(0));
                require(id > 0 && id <= lastId && orderIds.add(id), "订单号重复或超过系统记录");
                List<List<String>> items = details.remove(id);
                require(items != null && !items.isEmpty(), "订单缺少明细");
                List<Object> fields = new ArrayList<>(Arrays.asList(id, row.get(1), row.get(2),
                        XlsxFile.date(row.get(3)).getTime(), amount(row.get(4)), items.size()));
                List<Goods> snapshots = new ArrayList<>();
                for (List<String> item : items) {
                    Goods g = goods(item.subList(1, 9));
                    snapshots.add(g);
                    fields.add(g.toFileLine());
                    fields.add(integer(item.get(9)));
                }
                // 复用订单校验，不重复累计消费，也不关联当前售价。
                Order order = Order.fromFileLine(TextCodec.join(fields.toArray()), customers);
                for (int i = 0; i < snapshots.size(); i++) order.getGoods().get(i).setDOM(snapshots.get(i).getDOM());
                order.getCustomer().addOrder(order);
            }
            require(details.isEmpty(), "存在不属于任何订单的明细");

            return state;
        } catch (RuntimeException e) {
            throw new IOException(path.getFileName() + " 数据无效：" + e.getMessage(), e);
        }
    }

    private static List<List<String>> rows(Map<String, List<List<String>>> sheets, String name) {
        List<List<String>> rows = sheets.get(name);
        return rows.subList(1, rows.size());
    }
    private static Goods goods(List<String> row) {
        require(!row.get(0).isBlank(), "商品ID为空");
        return new Goods(row.get(0), row.get(1), row.get(2), row.get(3).isEmpty() ? null : XlsxFile.date(row.get(3)),
                row.get(4), amount(row.get(5)), amount(row.get(6)), integer(row.get(7)));
    }
    private static List<Object> goodsRow(Goods g) {
        return Arrays.asList(g.getGoodsID(), g.getGoodsName(), g.getFactory(), g.getDOM(), g.getModel(), g.getInPrice(), g.getOutPrice(), g.getStock());
    }
    private static void add(Map<String, List<List<Object>>> sheets, String name, Object... fields) { sheets.get(name).add(Arrays.asList(fields)); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalArgumentException(message); }
    private static int integer(String value) { int number = Integer.parseInt(value); require(number >= 0, "数量或编号不能为负"); return number; }
    private static double amount(String value) { double number = Double.parseDouble(value); require(Double.isFinite(number) && number >= 0, "金额无效"); return number; }
    private static boolean bool(String value) { require(value.equals("true") || value.equals("false"), "布尔值无效"); return Boolean.parseBoolean(value); }
    boolean hasSavedData() { return saved; }
    boolean shouldCreateDefaults() { return createDefaults; }
    @Override public void close() throws IOException { try { lock.release(); } finally { lockChannel.close(); } }
}
