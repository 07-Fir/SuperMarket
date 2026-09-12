package all_class;

import java.io.FilterOutputStream;
import java.lang.classfile.CustomAttribute;
import java.lang.runtime.SwitchBootstraps;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Year;
import java.util.*;

public class Main_System {
    private static Scanner sc = new Scanner(System.in);
    private static List<Administrators> admins = new ArrayList<>();
    private static List<Customer> customers = new ArrayList<>();
    private static List<Goods> goods = new ArrayList<>();
    private static Administrators currentAdmin = null;
    private static Customer currentCustomer = null;
    private static MockEmailSender emailSender = new MockEmailSender();


    public static void main(String[] args){
        admins.add(new Administrators("admin","ynuinfo#777"));

        goods.add(new Goods("A0001","智能手机", "华为",new Date(), "mate60", 2400.0, 2900.0, 50));
        goods.add(new Goods("A0002","蓝牙耳机", "小米",new Date(), "Air2", 120.0, 199.0, 100));
        goods.add(new Goods("A0003","笔记本电脑", "联想", new Date(), "ThinkPad X1", 4500.0, 5999.0, 20 ));

        System.out.println("=========== 欢迎使用购物管理系统 ===========");
        while (true) {
            showMainMenu();
            int choice = readInt("请选择操作：");
            try {
                switch (choice){
                    case 1:
                        if(currentCustomer != null){
                            System.out.println("登陆状态下不能登录管理员，请先退出登录");
                        }else adminLogin();
                        break;
                    case 2:
                        if(currentAdmin != null || currentCustomer != null){
                            System.out.println("登陆状态下不能注册顾客，请先退出登录");
                        }else customerRegister();
                        break;
                    case 3:
                        if(currentAdmin != null || currentCustomer != null) {
                            System.out.println("登陆状态下不能登录顾客，请先退出登录");
                        }else customerLogin();
                        break;
                    case 4:
                        if (currentAdmin != null) adminManageGoods();
                        else if(currentCustomer != null) customerBrowseGoods();
                        else System.out.println("请先登录！");
                        break;
                    case 5:
                        if (currentAdmin != null) adminManageCustomer();
                        else if(currentCustomer != null) customerManageCart();
                        else System.out.println("请先登录！");
                        break;
                    case 6:
                        if (currentAdmin != null) adminResetCustomerPW();
                        else if (currentCustomer != null) customerCheckout();
                        else System.out.println("请先登录！");
                        break;
                    case 7:
                        if (currentAdmin != null) adminChangePW();
                        else if (currentCustomer != null) customerViewHistory();
                        else System.out.println("请先登录！");
                        break;
                    case 8:
                        if (currentAdmin != null) System.out.println("此功能正在开发中,敬请等待");
                        else if (currentCustomer != null) customerModifyInformation();
                        else System.out.println("请先登录！");
                        break;
                    case 9:
                        if(currentAdmin != null) adminLogout();
                        else if (currentCustomer != null) customerLogout();
                        else System.out.println("您尚未登陆，亲");
                        break;
                    case 0:
                        System.out.println("感谢使用，欢迎下次再见！");
                        return;
                    default: System.out.println("无效选项，请重新选择");
                }
            } catch (Exception e){
                System.out.println("操作异常：" + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("\n按回车键继续...");
            sc.nextLine();
        }


    }
    private static void showMainMenu() {
        System.out.println("\n---------------主菜单---------------");
        if(currentCustomer == null || currentAdmin != null) {
            System.out.println("1.管理员登录");
            System.out.println("2.顾客注册");
            System.out.println("3.顾客登录");
        }

        if (currentAdmin != null){
            System.out.println("4.管理商品");
            System.out.println("5.管理顾客");
            System.out.println("6.重置顾客密码");
            System.out.println("7.修改管理员密码");
            System.out.println("8.待开发");
            System.out.println("9.退出登录");
        }
        else if (currentCustomer != null){
            System.out.println("4.浏览商品");
            System.out.println("5.管理购物车");
            System.out.println("6.结账");
            System.out.println("7.查看购物历史");
            System.out.println("8.修改个人信息");
            System.out.println("9.退出登录");
        }
        else {
            System.out.println("4. （请先登录）");
            System.out.println("5. （请先登录）");
            System.out.println("6. （请先登录）");
            System.out.println("7. （请先登录）");
            System.out.println("8. （请先登录）");
            System.out.println("9. （请先登录）");
        }

        System.out.println("0.退出系统");
        if (currentAdmin != null){
            System.out.println("当前管理员: " + currentAdmin.getAccount());
        }
        else if (currentCustomer != null){
            System.out.println("当前顾客: " + currentCustomer.getUserName() + "（级别:" + currentCustomer.getLevel() + "）");
        }
    }


    private static int readInt(String prompt) {
        while (true) {
            System.out.println(prompt);
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("请输入有效整数！");
            }
        }
    }

    private static double readDouble(String prompt){
        while (true){
            System.out.println(prompt);
            try {
                return Double.parseDouble(sc.nextLine().trim());
            } catch (NumberFormatException e){
                System.out.println("请输入有效数字！");
            }
        }
    }

    private static String readString(String prompt){
        System.out.println(prompt);
        return sc.nextLine().trim();
    }

    private static Date readDate(String prompt){
        System.out.println(prompt + "(格式 yyyy-MM-dd,直接回车默认今天)：");
        String input = sc.nextLine().trim();
        if (input.isEmpty()) return new Date();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.parse(input);
        }catch (Exception e){
            System.out.println("日期格式错误，使用今天");
            return new Date();
        }
    }

    private static boolean confirm(String prompt){
        while (true){
            String input = readString(prompt + "（是/否）");
            if (input.equalsIgnoreCase("是")) return true;
            else if(input.equalsIgnoreCase("否")) return false;
            else System.out.println("请输入是/否");
        }
    }
// 管理员所有功能
    private static void adminLogin(){
        if (currentAdmin != null) {System.out.println("已登录管理员" + currentAdmin.getAccount());return;}
        String acc = readString("账号：");
        String pw = readString("密码：");
        for (Administrators a : admins){
            if (a.getAccount().equals(acc) && a.getPassword().equals(pw)){
                currentAdmin = a;
                System.out.println("管理员登陆成功");
                if(currentAdmin.isDefaultPassword()){
                    System.out.println("\n 检测到您正在使用默认密码，为了账户安全，请立即修改密码！");
                    System.out.println("修改完成后才能继续使用管理员功能");
                    boolean changed = forceChangeDefaultPassword();
                    if (!changed){
                        currentAdmin = null;
                        System.out.println("未完成密码修改，已退出管理员登陆");
                    }
                }
                return;
            }
        }
        System.out.println("账号或密码错误!");
    }


    private static boolean forceChangeDefaultPassword(){
        while (true){
            System.out.println("\n---首次登陆，强制修改默认密码---");
            String oldPW = readString("请输入当前默认密码：");
            if (!currentAdmin.getPassword().equals(oldPW)){
                System.out.println("原密码错误，请重新输入密码");
                continue;
            }
            String newPW = readString("请输入新密码（长度>8,含大小写字母、数字和标点）：");
            if (!User.isPasswordSafe(newPW)){
                System.out.println("密码不符合复杂度要求,请重新输入");
                continue;
            }
            if (newPW.equals(oldPW)){
                System.out.println("新密码不能与默认密码相同,请重新输入");
            }
            String confirm = readString("请再次输入新密码：");
            if (!newPW.equals(confirm)) {
                System.out.println("两次输入不一致,请重新输入");
                continue;
            }
            currentAdmin.setPassword(newPW);
            currentAdmin.setDefaultPassword(false);
            System.out.println("密码修改成功！现在可以继续使用管理员功能了！");
            return true;
        }
    }

    private static void adminLogout(){
        if (currentAdmin != null){
            System.out.println("管理员" + currentAdmin.getAccount() + "已退出");
            currentAdmin = null;
        }
    }


    private static void adminChangePW(){
       if(currentAdmin == null){System.out.println("请先登录管理员");return;}
       String oldPW = readString("请输入当前密码：");
       if (!currentAdmin.getPassword().equals(oldPW)){
           System.out.println("原密码错误");
           return;
       }
       String newPW = readString("请输入新密码（长度>8,包含大小写字母、数字和标点）：");
       if (!User.isPasswordSafe(newPW)){
           System.out.println("密码不符合复杂度要求：");
           return;
       }
       String confirm = readString("请再次输入新密码：");
       if (!newPW.equals(confirm)){
           System.out.println("两次输入不一致！");
           return;
       }
       currentAdmin.setPassword(newPW);
       System.out.println("密码修改成功！");
    }

    private static void adminResetCustomerPW(){
        if (currentAdmin == null){System.out.println("请先登录管理员");return;}
        String username = readString("请输入要重置密码的顾客用户名：");
        Customer target = null;
        for (Customer c : customers){
            if (c.getUserName().equals(username)){
                target = c;
                break;
            }
        }
        if (target == null){
            System.out.println("未找到该顾客");
            return;
        }
        String newPW = User.randomPassword();
        target.setPassword(newPW);
        target.setLocked(false);
        target.setLoginTimes(0);
        emailSender.sendEmail(target.getEmail(),"您的账号密码已重置，新密码为：" + newPW);
        System.out.println("密码已重置并发送到顾客邮箱");
    }

    private static void adminManageCustomer(){
        if(currentAdmin == null) {System.out.println("请先登录管理员");return;}
        while (true){
            System.out.println("\n---顾客管理---");
            System.out.println("1.列出所有顾客");
            System.out.println("2.删除顾客");
            System.out.println("3.按ID或用户名查询顾客");
            System.out.println("4.返回上级");
            int choice = readInt("请选择：");
            switch (choice){
                case 1:
                    if (customers.isEmpty()) System.out.println("暂无顾客");
                    else customers.forEach(System.out::println);
                    break;
                case 2:
                    String uid = readString("请输入要删除的顾客ID：");
                    Customer toRemove = null;
                    for (Customer c : customers){
                        if (c.getUserID().equals(uid)){
                            toRemove = c;
                            break;
                        }
                    }
                    if (toRemove == null){
                        System.out.println("未找到该顾客");
                        break;
                    }
                    if (confirm("确认删除顾客" + toRemove.getUserName() + "吗？此操作不可恢复！")){
                        customers.remove(toRemove);
                        System.out.println("删除成功");
                    }else {
                        System.out.println("取消删除");
                    }
                    break;
                case 3:
                    String keyword = readString("请输入用户名或者ID （空则列出所有）：");
                    if (keyword.isEmpty()){
                       customers.forEach(System.out::println);
                       break;
                    }
                    boolean found = false;
                    for (Customer c : customers){
                        if (c.getUserName().equals(keyword) || c.getUserID().equals(keyword)){
                            System.out.println(c);
                            found = true;
                        }
                    }
                    if (!found) System.out.println("未找到匹配顾客");
                    break;
                case 4:
                    return;
                default:
                    System.out.println("无效输出");
            }
        }
    }


    private static void adminManageGoods(){
        if (currentAdmin == null){
            System.out.println("请先登录管理员");
            return;
        }
        while (true){
            System.out.println("\n---商品管理---");
            System.out.println("1.列出所有商品");
            System.out.println("2.添加商品");
            System.out.println("3.修改商品");
            System.out.println("4.删除商品");
            System.out.println("5.查询商品（名称/厂家/零售价组合）");
            System.out.println("6.返回上级");
            int choice = readInt("请选择：");
            switch (choice){
                case 1:
                    if (goods.isEmpty()) System.out.println("暂无商品");
                    else goods.forEach(g -> System.out.println(g.toAdminString()));
                    break;
                case 2:
                    addGoods();
                    break;
                case 3:
                    updateGoods();
                    break;
                case 4:
                    deleteGoods();
                    break;
                case 5:
                    searchGoods();
                    break;
                case 6:
                    return;
                default:
                    System.out.println("无效输入");
            }

        }
    }


    private static void addGoods(){
        String id = readString("商品编号：");
        for (Goods g : goods){
            if (g.getGoodsID().equals(id)){
                System.out.println("编号已存在！");
                return;
            }
        }
        String name = readString("商品名称：");
        String factory = readString("生产厂家：");
        Date dom = readDate("生产日期：");
        String model = readString("型号");
        double inPrice = readDouble("进货价：");
        double outPrice = readDouble("零售价：");
        int stock = readInt("库存量：");
        Goods g = new Goods(id, name, factory, dom, model, inPrice, outPrice, stock);
        goods.add(g);
        System.out.println("商品添加成功！:" + g.toAdminString());
    }

    private static void updateGoods(){
        String id = readString("请输入要修改的商品编号：");
        Goods target = null;
        for (Goods g : goods){
            if (g.getGoodsID().equals(id)){
                target = g;
                break;
            }
        }
        if (target == null){
            System.out.println("未找到商品");
            return;
        }
        System.out.println("当前信息：" + target.toAdminString());
        System.out.println("输入新值（直接回车保留原值）：");
        String name = readString("名称（" + target.getGoodsName() + "）：");
        if (!name.isEmpty()) target.setGoodsName(name);
        String factory = readString("厂家（" + target.getFactory() + "）：");
        if (!factory.isEmpty()) target.setFactory(factory);
        Date dom = readDate("生产日期（当前" + new SimpleDateFormat("yyyy-MM-dd").format(target.getDOM()) + "）：");
        if (dom != null) target.setDOM(dom);
        String model = readString("型号（" + target.getModel() + "）：");
        if (!model.isEmpty()) target.setModel(model);
        String inPriceStr = readString("进货价（" + target.getInPrice() + "）：");
        if (!inPriceStr.isEmpty()) target.setInPrice(Double.parseDouble(inPriceStr));
        String outPriceStr = readString("零售价（" + target.getOutPrice() + "）：");
        if (!outPriceStr.isEmpty()) target.setOutPrice(Double.parseDouble(outPriceStr));
        String stockStr = readString("库存（" + target.getStock() + "）：");
        if (!stockStr.isEmpty()) target.setStock(Integer.parseInt(stockStr));
        System.out.println("商品信息更新成功");
    }


    private static void deleteGoods(){
        String id = readString("请输入要删除的商品编号：");
        Goods target = null;
        for (Goods g : goods){
            if (g.getGoodsID().equals(id)){
                target = g;
                break;
            }
        }
        if (confirm("确认删除商品" + target.getGoodsName() + "吗？此操作不可恢复！")){
            goods.remove(target);
            System.out.println("删除成功！");
        }else System.out.println("取消删除");
    }


    private static void searchGoods(){
        String name = readString("商品名称（空表示不限）：");
        String factory = readString("生产厂家（空表示不限）");
        String priceRangeStr = readString("零售价格范围，格式：最低价-最高价（如 1000-3000,空表示不限）：");
        double minPrice = 0, maxPrice = Double.MAX_VALUE;
        if (!priceRangeStr.isEmpty()){
            try {
                String[] parts = priceRangeStr.split("-");
                minPrice = Double.parseDouble(parts[0]);
                maxPrice = (parts.length > 1) ? Double.parseDouble(parts[1]) : Double.MAX_VALUE;
            }catch (Exception e){
                System.out.println("价格格式错误,请仔细查看价格条件");
            }
        }
        List<Goods> result = new ArrayList<>();
        for (Goods g : goods){
            boolean match = true;
            if (!name.isEmpty() && !g.getGoodsName().contains(name))  match = false;
            if (!factory.isEmpty() && !g.getFactory().contains(factory)) match = false;
            if (g.getOutPrice() < minPrice || g.getOutPrice() > maxPrice) match = false;
            if (match) result.add(g);
        }
        if (result.isEmpty()){
            System.out.println("未找到匹配的商品");
        }else {
            System.out.println("匹配的商品如下：");
            result.forEach(g -> System.out.println(g.toAdminString()));
        }
    }
// 顾客所有功能
    private static void customerRegister(){
        String username = readString("用户名（至少5个字符）：");
        if (username.length() < 5){
            System.out.println("用户名长度不足5！");
            return;
        }
        for (Customer c : customers){
            if (c.getUserName().equals(username)){
                System.out.println("用户名已被使用！");
                return;
            }
        }
        String pwd = readString("密码（长度>8,含大小写字母、数字和标点）：");
        if (!User.isPasswordSafe(pwd)){
            System.out.println("密码不符合复杂度要求！");
            return;
        }
        String phone;
        while (true){
            phone = readString("手机号：");
            if (phone.equalsIgnoreCase("q")){
                System.out.println("已取消注册");
                return;
            }
            if (User.isPhoneRight(phone)) break;
            System.out.println("手机号格式错误! 应为11位数字,1开头,第二位3-9  如果想取消注册账户,可输入q来退出");
        }
        String email;
        while (true){
            email = readString("邮箱：");
            if (email.equalsIgnoreCase("q")){
                System.out.println("已取消注册");
                return;
            }
            if (User.isEmailRight(email)) break;
            System.out.println("邮箱格式错误！ 示例：user@medium.com  如果想取消注册账户,可输入q来退出");
        }
        Customer newCustomer = new Customer(username, pwd, phone, email);
        customers.add(newCustomer);
        System.out.println("注册成功！您的ID为" + newCustomer.getUserID());
    }

    private static void customerLogin(){
        if (currentCustomer != null) {System.out.println("已登录顾客" + currentCustomer.getUserName()); return;}
        String username = readString("用户名：");
        Customer target = null;
        for (Customer c : customers){
            if (c.getUserName().equals(username)){
                target = c;
                break;
            }
        }
        if (target == null){
            System.out.println("用户名不存在！");
            return;
        }
        if (target.isLocked()){
            System.out.println("账户已被锁定，请联系管理员重置密码");
            return;
        }
        System.out.println("是否要重置密码（是/否）【忘记密码就写是】[回车默认为否]");
        String forgetPW = sc.nextLine().trim();
        if (forgetPW.equalsIgnoreCase("是")){
            custmerResetPW(target);
            return;
        }
        System.out.println("请输入密码：");
        String pwd = sc.nextLine().trim();
        if (target.getPassword().equals(pwd)){
            target.setLoginTimes(0);
            currentCustomer = target;
            System.out.println("登录成功！欢迎" + target.getUserName());
        }else {
            target.setLoginTimes(target.getLoginTimes() + 1);
            if (target.getLoginTimes() >= 3){
                target.setLocked(true);
                System.out.println("连续三次密码错误，账户已被锁定！");
            }else {
                System.out.println("密码错误，剩余尝试次数：" + (3 - target.getLoginTimes()));
            }
        }
    }


    private static void custmerResetPW(Customer customer){
        System.out.println("---重置密码---");
        String phone = readString("请输入注册手机号：");
        String email = readString("请输入注册邮箱");
        if (!customer.getPhone().equals(phone) || !customer.getEmail().equals(email)){
            System.out.println("个人信息验证失败，无法重置密码");
            return;
        }
        String newPW = User.randomPassword();
        customer.setPassword(newPW);
        customer.setLocked(false);
        customer.setLoginTimes(0);
        emailSender.sendEmail(customer.getEmail(), "您已重置密码,新密码为：" + newPW);
        System.out.println("密码已重置并发送至您的邮箱，请使用新密码重新登录");
    }


    private static void customerLogout(){
        if (currentCustomer != null){
            System.out.println("顾客" + currentCustomer.getUserName() + "已退出");
            currentCustomer = null;
        }
    }


    private static void customerModifyInformation(){
        if (currentCustomer == null){System.out.println("请先登录"); return;}
        System.out.println("---修改个人信息---");
        System.out.println("输入新值（回车默认保留原值）：");
        String username = readString("用户名（" + currentCustomer.getUserName() + "）：");
        if (!username.isEmpty()){
            if (username.length() < 5){
                System.out.println("用户名长度不足5,修改取消");
                return;
            }
            for (Customer c : customers){
                if (c != currentCustomer && c.getUserName().equals(username)){
                    System.out.println("用户名已被占用！");
                    return;
                }
            }
            currentCustomer.setUserName(username);
        }
        String phone = readString("手机号（" + currentCustomer.getPhone() + "）：");
        if (!phone.isEmpty()) {
            if (!User.isPhoneRight(phone)) {
                System.out.println("手机号格式错误！应为11位数字，1开头，第二位3-9");
                return;
            }
            currentCustomer.setPhone(phone);
        }
        String email = readString("邮箱（" + currentCustomer.getEmail() + "）：");
        if (!email.isEmpty()) {
            if (!User.isEmailRight(email)){
                System.out.println("邮箱格式错误！示例：user@medium.com");
                return;
            }
            currentCustomer.setEmail(email);
        }
        String changePW = readString("是否修改密码（是/否）（回车默认否）：");
        if (changePW.equalsIgnoreCase("是")){
            String oldPW = readString("请输入原密码：");
            if (!currentCustomer.getPassword().equals(oldPW)){
                System.out.println("原密码错误！");
                return;
            }
            String newPW = readString("新密码（长度>8,含大小写字母、数字和标点）");
            if (!User.isPasswordSafe(newPW)){
                System.out.println("密码不符合复杂度要求!");
                return;
            }
            String confirm = readString("请再次输入新密码：");
            if (!newPW.equals(confirm)){
                System.out.println("两次输入不一致！");
                return;
            }
            currentCustomer.setPassword(newPW);
            System.out.println("密码修改成功！");
        }
        System.out.println("个人信息修改完成");
    }


    private static void customerBrowseGoods(){
        if (currentCustomer == null){System.out.println("请先登录"); return;}
        System.out.println("\n---商品列表---");
        if (goods.isEmpty()){System.out.println("暂无商品"); return;}
        goods.forEach(System.out::println);
    }


    private static void customerManageCart() {
        if (currentCustomer == null) {
            System.out.println("请先登录");
            return;
        }
        while (true) {
            System.out.println("\n---购物车管理---");
            System.out.println("1.查看购物车");
            System.out.println("2.添加商品到购物车");
            System.out.println("3.从购物车中移除商品");
            System.out.println("4.修改购物车商品数量");
            System.out.println("5.返回上级");
            int choice = readInt("请选择：");
            switch (choice){
                case 1 -> viewCart();
                case 2 -> addToCart();
                case 3 -> removeFromCart();
                case 4 -> modifyCartItem();
                case 5 -> {
                    return;
                }
                default -> System.out.println("无效输入");
            }
        }
    }


    private static void viewCart(){
        Map<Goods, Integer> cart = currentCustomer.getCart();
        if (cart.isEmpty()){
            System.out.println("购物车为空");
            return;
        }
        System.out.println("---购物车---");
        double total = 0;
        for (Map.Entry<Goods, Integer> entry : cart.entrySet()){
            Goods g = entry.getKey();
            int amount = entry.getValue();
            System.out.println(g.getGoodsID() + "：" + g.getGoodsName() + "x" + amount + "单价：" + g.getOutPrice() + "小计：" + g.getOutPrice() * amount);
            total += g.getOutPrice() * amount;
        }
        System.out.println("总计" + total);
    }


    private static void addToCart(){
        String id = readString("输入添加的商品编号：");
        Goods target = null;
        for (Goods g : goods){
            if (g.getGoodsID().equals(id)){
                target = g;
                break;
            }
        }
        if (target == null){
            System.out.println("未找到该商品");
            return;
        }
        int amount = readInt("请输入数量：");
        if (amount <= 0){
            System.out.println("数量必须大于0");
            return;
        }
        if (amount > target.getStock()){
            System.out.println("库存不足,当前库存数：" + target.getStock());
            return;
        }
        Map<Goods, Integer> cart = currentCustomer.getCart();
        cart.put(target, cart.getOrDefault(target, 0) + amount);
        System.out.println("已添加" + target.getGoodsName() + "x" + amount + "到购物车");
    }


    private static void removeFromCart(){
        Map<Goods, Integer> cart = currentCustomer.getCart();
        if (cart.isEmpty()){
            System.out.println("购物车为空");
            return;
        }
        viewCart();
        String id = readString("请输入要移除的商品编号：");
        Goods target = null;
        for (Goods g : cart.keySet()){
            if (g.getGoodsID().equals(id)){
                target = g;
                break;
            }
        }
        if (target == null){
            System.out.println("购物车中没有此商品");
            return;
        }
        if (confirm("确认从购物车中移除：" + target.getGoodsName() + "吗？")){
            cart.remove(target);
            System.out.println("移除成功");
        }else System.out.println("取笑移除");
    }


    private static void modifyCartItem(){
        Map<Goods, Integer> cart = currentCustomer.getCart();
        if (cart.isEmpty()){
            System.out.println("购物车为空");
            return;
        }
        viewCart();
        String id = readString("请输入要修改数量的商品编号：");
        Goods target = null;
        for (Goods g : cart.keySet()){
            if (g.getGoodsID().equals(id)){
                target = g;
                break;
            }
        }
        if (target == null){
            System.out.println("购物车中没有此商品");
            return;
        }
        int newAmount = readInt("请输入新的数量（输入0或负数将移除该商品）：");
        if (newAmount <= 0){
            cart.remove(target);
            System.out.println("商品已从购物车中移除");
        }else {
            if (newAmount > target.getStock()){
                System.out.println("库存不足，当前库存数：" + target.getStock());
                return;
            }
            cart.put(target, newAmount);
            System.out.println("商品数量修改成功");
        }
    }


    private static void customerCheckout(){
        if (currentCustomer == null){System.out.println("请先登录"); return;}
        Map<Goods, Integer> cart = currentCustomer.getCart();
        if (cart.isEmpty()){
            System.out.println("购物车为空，无法支付");
            return;
        }
        viewCart();
        int payChoice = readInt("请选择支付渠道 （1.支付宝 2.微信 3.银行卡,选择数字进行填写）：");
        String method;
        switch (payChoice){
            case 1 -> method = "支付宝";
            case 2 -> method = "微信";
            case 3 -> method = "银行卡";
            default -> {
                System.out.println("无效选择，取消结账");
                return;
            }
        }
        List<Goods> orderGoods = new ArrayList<>();
        Map<String, Integer> countMap = new HashMap<>();
        for (Map.Entry<Goods, Integer> entry : cart.entrySet()){
            Goods g = entry.getKey();
            int need = entry.getValue();
            if (g.getStock() < need){
                System.out.println("商品" + g.getGoodsID() + "：" + g.getGoodsName() + "库存不足（需求" + need + ", 库存" + g.getStock() + "）, 结账失败");
                return;
            }
        }
        for (Map.Entry<Goods, Integer> entry : cart.entrySet()){
            Goods g = entry.getKey();
            int need = entry.getValue();
            g.setStock(g.getStock() - need);
            orderGoods.add(g);
            countMap.put(g.getGoodsID(), need);
        }
        Order order = new Order(currentCustomer, orderGoods, countMap, method);
        currentCustomer.addOrder(order);
        cart.clear();
        System.out.println("结账成功！订单已生成");
        System.out.println(order.getPay());
    }

    private static void customerViewHistory(){
        if (currentCustomer == null){System.out.println("请先登录"); return;}
        List<Order> history = currentCustomer.getOrderHistory();
        if (history.isEmpty()){
            System.out.println("暂无购物记录");
            return;
        }
        System.out.println("---购物历史---");
        for (Order o : history){
            System.out.println(o.getPay());
        }
    }
}

