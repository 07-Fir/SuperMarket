package all_class;

import java.io.IOException;
import java.nio.channels.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

/** 4.0：SQLite 事务保存完整状态；所有用户输入均通过预编译参数写入。 */
final class SqliteStore implements AutoCloseable {
    private final Path directory, file;
    private final FileChannel lockChannel;
    private final FileLock lock;
    private Connection connection;
    private boolean loaded, saved, createDefaults;

    static final class State {
        List<Administrators> admins = new ArrayList<>();
        List<Customer> customers = new ArrayList<>();
        List<Goods> goods = new ArrayList<>();
        int lastOrderId = 1000;
    }

    SqliteStore(Path directory) throws IOException {
        this.directory = directory.toAbsolutePath().normalize();
        file = this.directory.resolve("supermarket-data.db");
        try { Class.forName("org.sqlite.JDBC"); }
        catch (ClassNotFoundException e) { throw new IOException("缺少 SQLite 驱动，请将 lib 中的 sqlite-jdbc 加入运行依赖", e); }
        Files.createDirectories(this.directory);
        // 与 3.0 使用同一把锁，防止迁移期间另一进程修改 Excel。
        lockChannel = FileChannel.open(this.directory.resolve("supermarket-data.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            lock = lockChannel.tryLock();
            if (lock == null) throw new IOException("另一个程序正在使用此数据目录，请先关闭它");
        } catch (IOException | OverlappingFileLockException e) {
            lockChannel.close();
            throw new IOException("无法独占数据目录，请检查是否已启动另一个程序", e);
        }
    }

    State load() throws IOException {
        loaded = false;
        if (connection != null) throw new IOException("本次运行已打开数据库，请重新启动后再读取");
        if (!Files.exists(file)) {
            State imported = importOldData();
            loaded = true;
            return imported;
        }
        try {
            connection = open(file);
            connection.setAutoCommit(false);
            checkIntegrity(connection);
            State state = readState(connection);
            connection.commit();
            connection.setAutoCommit(true);
            saved = true;
            loaded = true;
            createDefaults = false;
            return state;
        } catch (SQLException | RuntimeException e) {
            rollback(connection, e);
            throw new IOException("数据库读取失败，禁止覆盖；不会自动使用旧版文件替代当前数据", e);
        }
    }

    private State importOldData() throws IOException {
        State state = new State();
        Path excel = directory.resolve("supermarket-data.xlsx");
        Path backup = directory.resolve("supermarket-data.xlsx.bak");
        if (Files.exists(excel) || Files.exists(backup)) {
            ExcelStore.State old;
            try { old = ExcelStore.readForMigration(excel); }
            catch (IOException primaryError) {
                try { old = ExcelStore.readForMigration(backup); }
                catch (IOException backupError) {
                    primaryError.addSuppressed(backupError);
                    throw new IOException("Excel 和备份都无法读取，已停止创建数据库", primaryError);
                }
                System.out.println("Excel 读取失败，将从上一份 Excel 备份导入，最近一次修改可能丢失。");
            }
            state.admins = old.admins;
            state.customers = old.customers;
            state.goods = old.goods;
            state.lastOrderId = old.lastOrderId;
            createDefaults = false;
            System.out.println("已读取 3.0 Excel 数据，首次保存时将迁移至 SQLite。");
        } else {
            BinaryStore oldStore = new BinaryStore(directory);
            BinaryStore.State old = oldStore.load();
            state.admins = old.admins;
            state.customers = old.customers;
            state.goods = old.goods;
            state.lastOrderId = old.lastOrderId;
            createDefaults = oldStore.shouldCreateDefaults();
        }
        return state;
    }

    void save(List<Administrators> admins, List<Customer> customers, List<Goods> goods, int lastOrderId) throws IOException {
        if (!loaded) throw new IOException("未成功读取数据，不能覆盖保存");
        if (connection == null) {
            // 首次建库在临时文件中完成，失败时不会留下一个被误认为正式数据的空库。
            Path temp = Files.createTempFile(directory, "supermarket-data-", ".tmp");
            try {
                try (Connection initial = open(temp)) { writeState(initial, true, admins, customers, goods, lastOrderId); }
                if (Files.exists(file)) throw new IOException("数据库已被其他程序创建，请重新启动后读取");
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE);
                connection = open(file);
                saved = true;
            } catch (SQLException | RuntimeException e) {
                loaded = false;
                throw new IOException("数据库初始化失败，原数据未被覆盖", e);
            } catch (IOException e) {
                loaded = false;
                throw e;
            } finally {
                Files.deleteIfExists(temp);
                Files.deleteIfExists(Path.of(temp + "-journal"));
            }
        } else {
            try {
                writeState(connection, false, admins, customers, goods, lastOrderId);
                saved = true;
            } catch (SQLException | RuntimeException e) {
                loaded = false;
                throw new IOException("数据库保存失败，本次事务已回滚，请重新启动程序", e);
            }
        }
    }

    private static Connection open(Path path) throws SQLException {
        Connection db = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
        try (Statement s = db.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
            s.execute("PRAGMA busy_timeout = 5000");
            s.execute("PRAGMA journal_mode = DELETE");
            s.execute("PRAGMA synchronous = FULL");
            s.execute("PRAGMA secure_delete = ON");
            return db;
        } catch (SQLException e) { db.close(); throw e; }
    }

    private static void createSchema(Connection db) throws SQLException {
        String[] schema = {
            "CREATE TABLE metadata (id INTEGER PRIMARY KEY CHECK(id=1), schema_version INTEGER NOT NULL CHECK(schema_version=4), last_order_id INTEGER NOT NULL CHECK(last_order_id>=1000)) STRICT",
            "CREATE TABLE admins (account TEXT PRIMARY KEY NOT NULL CHECK(length(account)>0), password_hash TEXT NOT NULL, default_password INTEGER NOT NULL CHECK(default_password IN (0,1))) STRICT",
            "CREATE TABLE customers (user_id TEXT PRIMARY KEY NOT NULL CHECK(length(user_id)>0), username TEXT NOT NULL, password_hash TEXT NOT NULL, phone TEXT NOT NULL, email TEXT NOT NULL, registered_at INTEGER NOT NULL, total_consumption REAL NOT NULL CHECK(total_consumption>=0), failed_logins INTEGER NOT NULL CHECK(failed_logins>=0), locked INTEGER NOT NULL CHECK(locked IN (0,1))) STRICT",
            "CREATE TABLE goods (goods_id TEXT PRIMARY KEY NOT NULL CHECK(length(goods_id)>0), name TEXT NOT NULL, factory TEXT NOT NULL, produced_at INTEGER, model TEXT NOT NULL, in_price REAL NOT NULL CHECK(in_price>=0), out_price REAL NOT NULL CHECK(out_price>=0), stock INTEGER NOT NULL CHECK(stock>=0)) STRICT",
            "CREATE TABLE cart (user_id TEXT NOT NULL REFERENCES customers(user_id), goods_id TEXT NOT NULL REFERENCES goods(goods_id), quantity INTEGER NOT NULL CHECK(quantity>0), PRIMARY KEY(user_id,goods_id)) STRICT",
            "CREATE TABLE orders (order_id INTEGER PRIMARY KEY CHECK(order_id>0), user_id TEXT NOT NULL REFERENCES customers(user_id), payment_method TEXT NOT NULL, bought_at INTEGER NOT NULL, total_amount REAL NOT NULL CHECK(total_amount>=0)) STRICT",
            // 历史商品快照不关联当前商品表，商品删除或改价不应改变历史订单。
            "CREATE TABLE order_items (order_id INTEGER NOT NULL REFERENCES orders(order_id), item_index INTEGER NOT NULL CHECK(item_index>=0), goods_id TEXT NOT NULL, name TEXT NOT NULL, factory TEXT NOT NULL, produced_at INTEGER, model TEXT NOT NULL, in_price REAL NOT NULL CHECK(in_price>=0), out_price REAL NOT NULL CHECK(out_price>=0), stock INTEGER NOT NULL CHECK(stock>=0), quantity INTEGER NOT NULL CHECK(quantity>0), PRIMARY KEY(order_id,item_index), UNIQUE(order_id,goods_id)) STRICT",
            "PRAGMA user_version = 4"
        };
        try (Statement s = db.createStatement()) { for (String sql : schema) s.execute(sql); }
    }

    private static void writeState(Connection db, boolean first, List<Administrators> admins,
                                   List<Customer> customers, List<Goods> goods, int lastOrderId) throws SQLException {
        db.setAutoCommit(false);
        try {
            if (first) createSchema(db);
            try (Statement s = db.createStatement()) {
                for (String table : List.of("order_items", "cart", "orders", "customers", "goods", "admins", "metadata"))
                    s.executeUpdate("DELETE FROM " + table);
            }
            for (Administrators a : admins)
                insert(db, "admins", a.getAccount(), PasswordHash.requireEncoded(a.getPasswordHash()), a.isDefaultPassword() ? 1 : 0);
            for (Customer c : customers)
                insert(db, "customers", c.getUserID(), c.getUserName(), PasswordHash.requireEncoded(c.getPasswordHash()),
                        c.getPhone(), c.getEmail(), c.getRegisterTime().getTime(), finite(c.getTotalConsumption()), c.getLoginTimes(), c.isLocked() ? 1 : 0);
            for (Goods g : goods)
                insert(db, "goods", g.getGoodsID(), g.getGoodsName(), g.getFactory(), millis(g.getDOM()), g.getModel(),
                        finite(g.getInPrice()), finite(g.getOutPrice()), g.getStock());
            for (Customer c : customers) {
                for (Map.Entry<Goods, Integer> item : c.getCart().entrySet())
                    insert(db, "cart", c.getUserID(), item.getKey().getGoodsID(), item.getValue());
                for (Order order : c.getOrderHistory()) {
                    require(order.getCustomer() == c, "订单所属用户不一致");
                    insert(db, "orders", order.getOrderID(), c.getUserID(), order.getPaymentMethod(), order.getBuyTime().getTime(), finite(order.getTotalAmount()));
                    int index = 0;
                    for (Goods g : order.getGoods()) {
                        Integer quantity = order.getGoodsCountMap().get(g.getGoodsID());
                        if (quantity == null || quantity == 0) continue;
                        insert(db, "order_items", order.getOrderID(), index++, g.getGoodsID(), g.getGoodsName(), g.getFactory(),
                                millis(g.getDOM()), g.getModel(), finite(g.getInPrice()), finite(g.getOutPrice()), g.getStock(), quantity);
                    }
                }
            }
            insert(db, "metadata", 1, 4, lastOrderId);
            readState(db); // 在提交前检查密码格式、引用关系和订单金额。
            db.commit();
            db.setAutoCommit(true);
        } catch (SQLException | RuntimeException e) {
            rollback(db, e);
            throw e;
        }
    }

    private static State readState(Connection db) throws SQLException {
        State state = new State();
        int lastId;
        try (Statement s = db.createStatement(); ResultSet r = s.executeQuery("PRAGMA user_version")) {
            require(r.next() && r.getInt(1) == 4, "数据库版本不是 4.0");
        }
        try (Statement s = db.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM metadata")) {
            require(r.next() && r.getInt("id") == 1 && r.getInt("schema_version") == 4, "系统信息缺失");
            lastId = integer(r, "last_order_id");
            require(lastId >= 1000 && !r.next(), "系统信息无效");
        }
        try (Statement s = db.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM admins ORDER BY rowid")) {
            while (r.next()) {
                Administrators a = Administrators.fromPasswordHash(r.getString("account"), r.getString("password_hash"));
                a.setDefaultPassword(flag(r, "default_password"));
                state.admins.add(a);
            }
        }
        require(!state.admins.isEmpty(), "管理员数据缺失");
        Map<String, Customer> customers = new LinkedHashMap<>();
        try (Statement s = db.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM customers ORDER BY rowid")) {
            while (r.next()) {
                Customer c = Customer.fromPasswordHash(r.getString("username"), r.getString("password_hash"), r.getString("phone"), r.getString("email"));
                c.setUserID(r.getString("user_id"));
                c.setRegisterTime(new Date(r.getLong("registered_at")));
                c.setTotalConsumption(finite(r.getDouble("total_consumption")));
                c.setLoginTimes(integer(r, "failed_logins"));
                c.setLocked(flag(r, "locked"));
                customers.put(c.getUserID(), c);
            }
        }
        state.customers.addAll(customers.values());
        Map<String, Goods> goods = new LinkedHashMap<>();
        try (Statement s = db.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM goods ORDER BY rowid")) {
            while (r.next()) { Goods g = goods(r); goods.put(g.getGoodsID(), g); }
        }
        state.goods.addAll(goods.values());
        try (Statement s = db.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM cart")) {
            while (r.next()) {
                Customer c = customers.get(r.getString("user_id"));
                Goods g = goods.get(r.getString("goods_id"));
                int quantity = integer(r, "quantity");
                require(c != null && g != null && quantity > 0, "购物车数据无效");
                c.getCart().put(g, quantity);
            }
        }
        Map<Integer, List<Goods>> items = new HashMap<>();
        Map<Integer, Map<String, Integer>> quantities = new HashMap<>();
        try (Statement s = db.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM order_items ORDER BY order_id,item_index")) {
            while (r.next()) {
                int id = integer(r, "order_id");
                Goods g = goods(r);
                int quantity = integer(r, "quantity");
                require(quantity > 0, "订单数量无效");
                items.computeIfAbsent(id, key -> new ArrayList<>()).add(g);
                quantities.computeIfAbsent(id, key -> new LinkedHashMap<>()).put(g.getGoodsID(), quantity);
            }
        }
        try (Statement s = db.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM orders ORDER BY order_id")) {
            while (r.next()) {
                int id = integer(r, "order_id");
                require(id <= lastId, "订单号超过系统记录");
                Customer c = customers.get(r.getString("user_id"));
                Order order = Order.restore(id, c, items.remove(id), quantities.remove(id), r.getString("payment_method"),
                        new Date(r.getLong("bought_at")), finite(r.getDouble("total_amount")));
                c.addOrder(order);
            }
        }
        require(items.isEmpty(), "存在无主订单明细");
        state.lastOrderId = lastId;
        return state;
    }

    private static void checkIntegrity(Connection db) throws SQLException {
        try (Statement s = db.createStatement(); ResultSet r = s.executeQuery("PRAGMA quick_check")) {
            require(r.next() && "ok".equals(r.getString(1)) && !r.next(), "数据库完整性检查失败");
        }
        try (Statement s = db.createStatement(); ResultSet r = s.executeQuery("PRAGMA foreign_key_check")) {
            require(!r.next(), "数据库引用关系损坏");
        }
    }

    private static Goods goods(ResultSet r) throws SQLException {
        long time = r.getLong("produced_at");
        Date date = r.wasNull() ? null : new Date(time);
        return new Goods(r.getString("goods_id"), r.getString("name"), r.getString("factory"), date, r.getString("model"),
                finite(r.getDouble("in_price")), finite(r.getDouble("out_price")), integer(r, "stock"));
    }
    private static void insert(Connection db, String table, Object... values) throws SQLException {
        String placeholders = String.join(",", Collections.nCopies(values.length, "?"));
        try (PreparedStatement p = db.prepareStatement("INSERT INTO " + table + " VALUES (" + placeholders + ")")) {
            for (int i = 0; i < values.length; i++) p.setObject(i + 1, values[i]);
            p.executeUpdate();
        }
    }
    private static Long millis(Date date) { return date == null ? null : date.getTime(); }
    private static double finite(double value) { require(Double.isFinite(value) && value >= 0, "金额无效"); return value; }
    private static int integer(ResultSet r, String column) throws SQLException {
        long value = r.getLong(column);
        require(!r.wasNull() && value >= 0 && value <= Integer.MAX_VALUE, "数量或编号无效");
        return (int) value;
    }
    private static boolean flag(ResultSet r, String column) throws SQLException {
        int value = integer(r, column); require(value <= 1, "布尔值无效"); return value == 1;
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalArgumentException(message); }
    private static void rollback(Connection db, Exception cause) {
        if (db != null) try { db.rollback(); } catch (SQLException e) { cause.addSuppressed(e); }
    }
    boolean hasSavedData() { return saved; }
    boolean shouldCreateDefaults() { return createDefaults; }
    @Override public void close() throws IOException {
        try {
            if (connection != null) try { connection.close(); } catch (SQLException e) { throw new IOException("关闭数据库失败", e); }
        } finally { try { lock.release(); } finally { lockChannel.close(); } }
    }
}
