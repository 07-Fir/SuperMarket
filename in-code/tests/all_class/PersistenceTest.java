package all_class;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

// 不使用测试框架，直接运行 main。测试数据与正式数据分开存放。
public class PersistenceTest {
    private static int passed = 0;

    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
        passed++;
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(".test-build");
        Files.createDirectories(root);
        Path dir = Files.createTempDirectory(root, "simple-test-");

        // 1. 导入旧文件，原文件应保持不变。
        String oldCustomer = "id001|student|Abc12345!|13800000000|test@example.com|2026-09-13 12:00:00|0";
        Files.writeString(dir.resolve("customers.txt"), oldCustomer);
        Files.writeString(dir.resolve("admin.txt"), "admin|Admin123!|false");
        Files.writeString(dir.resolve("goods.txt"), "A0001|苹果|果园|2026-09-01|普通|2.0|5.0|10");
        TxtStore store = new TxtStore(dir);
        TxtStore.State state = store.load();
        check(state.customers.size() == 1 && state.goods.size() == 1, "旧数据未完整读入");
        Customer customer = state.customers.get(0);
        Goods goods = state.goods.get(0);

        // 2. 特殊字符、购物车、订单必须能在重启后恢复。
        String password = "Abc12345|\\!";
        customer.setPassword(password);
        customer.setUserName("学生|\\\n\t");
        customer.getCart().put(goods, 2);
        Order order = new Order(customer, state.goods, Map.of("A0001", 2), "微信");
        customer.addOrder(order);
        customer.addConsumption(order.getTotalAmount());
        store.save(state.admins, state.customers, state.goods);
        check(Files.readString(dir.resolve("customers.txt")).equals(oldCustomer), "原文件被修改");
        String receipt = order.getPay();

        store = new TxtStore(dir); // 用新对象模拟重新启动。
        state = store.load();
        customer = state.customers.get(0);
        goods = state.goods.get(0);
        check(customer.getPassword().equals(password), "特殊字符密码未还原");
        check(customer.getUserName().equals("学生|\\\n\t"), "特殊字符用户名未还原");
        check(customer.getCart().get(goods) == 2, "购物车未恢复");
        check(customer.getOrderHistory().get(0).getPay().equals(receipt), "订单明细发生变化");
        check(customer.getTotalConsumption() == 10, "恢复订单重复增加了消费金额");
        goods.setOutPrice(20);
        check(customer.getOrderHistory().get(0).getGoods().get(0).getOutPrice() == 5, "改价影响历史订单");
        customer.setPassword("Changed123!");
        store.save(state.admins, state.customers, state.goods);

        // 3. 遗留的未写完临时文件，不应影响正式文件。
        Files.writeString(dir.resolve("supermarket-data.txt.tmp"), "没写完的数据");
        state = new TxtStore(dir).load();
        check(state.customers.get(0).getPassword().equals("Changed123!"), "错误地读取了临时文件");

        // 4. 正式文件损坏时读取备份，不能拿坏文件覆盖好备份。
        Files.writeString(dir.resolve("supermarket-data.txt"), "损坏的数据");
        store = new TxtStore(dir);
        state = store.load();
        check(state.customers.get(0).getPassword().equals(password), "没有恢复上一份备份");
        String backup = Files.readString(dir.resolve("supermarket-data.txt.bak"));
        store.save(state.admins, state.customers, state.goods);
        check(Files.readString(dir.resolve("supermarket-data.txt.bak")).equals(backup), "坏文件覆盖了备份");

        // 5. 两份文件都损坏时，读取失败后必须拒绝保存。
        Files.writeString(dir.resolve("supermarket-data.txt"), "坏文件");
        Files.writeString(dir.resolve("supermarket-data.txt.bak"), "坏备份");
        store = new TxtStore(dir);
        boolean failed = false;
        try { store.load(); } catch (IOException e) { failed = true; }
        check(failed, "损坏的数据被当作正常数据");
        failed = false;
        try { store.save(state.admins, state.customers, state.goods); }
        catch (IOException e) { failed = true; }
        check(failed && Files.readString(dir.resolve("supermarket-data.txt")).equals("坏文件"), "读取失败后仍覆盖了原文件");

        // 6. 格式错误的旧记录不能被悄悄跳过。
        Path badOld = dir.resolve("bad-old");
        Files.createDirectories(badOld);
        Files.writeString(badOld.resolve("customers.txt"), "bad|record");
        failed = false;
        try { new TxtStore(badOld).load(); } catch (IOException e) { failed = true; }
        check(failed, "格式错误的旧记录被跳过");
        System.out.println("通过 " + passed + " 项检查。测试目录：" + dir);
    }
}
