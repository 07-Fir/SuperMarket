package all_class;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main_System {
    private static Scanner sc = new Scanner(System.in);
    private static List<Administrators> admins = new ArrayList<>();
    private static List<Customer> customers = new ArrayList<>();
    private static List<Goods> goodsList  = new ArrayList<>();
    private static Administrators currentAdmin = null;
    private static Customer currentCustomer = null;
    private static MockEmailSender emailSender = new MockEmailSender();

    private static BinaryStore store;

    public static void main(String[] args) {
        // 在 IDEA 中将 Working directory 设置为项目根目录。
        java.nio.file.Path directory = java.nio.file.Path.of(
                System.getProperty("supermarket.dataDir", ".")).toAbsolutePath().normalize();
        System.out.println("数据目录：" + directory);
        try {
            store = new BinaryStore(directory);
            loadAllData();
            runMenu();
        } catch (IOException | UncheckedIOException e) {
            System.out.println("数据操作失败，程序已停止以保护已保存的数据：" + e.getMessage());
        }
    }

    private static void runMenu() {
        System.out.println("=========== 欢迎使用购物管理系统 2.0 ===========");
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
                        saveAllData();
                        System.out.println("数据已保存,感谢使用，欢迎下次再见！");
                        return;
                    default: System.out.println("无效选项，请重新选择");
                }
            } catch (UncheckedIOException e) {
                throw e;
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



// 保存完整数据；失败后停止程序，避免继续使用尚未提交的内存数据。
    private static void saveAllData() {
        try {
            store.save(admins, customers, goodsList);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void loadAllData() {
        try {
            BinaryStore.State state = store.load();
            admins = state.admins;
            customers = state.customers;
            goodsList = state.goods;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // 首次启动添加管理员文件（管理员文件不存在）, 创建默认管理员
        if (admins.isEmpty() && store.shouldCreateDefaults()){
            admins.add(new Administrators("admin", "ynuinfo#777"));
            System.out.println("首次启动，已创建默认管理员");
        }
        // 首次启动添加商品文件（商品文件不存在）
        if (goodsList.isEmpty() && store.shouldCreateDefaults()){
                // 定义 A-Z 对应的 26 个商品类别（A 代表电子产品）
                String[] categories = {
                        "电子产品", "服饰鞋帽", "食品生鲜", "家居用品", "图书文娱", "美妆个护",
                        "运动户外", "玩具乐器", "家用电器", "珠宝首饰", "宠物用品", "办公文具",
                        "汽车用品", "医药健康", "母婴用品", "箱包皮具", "五金工具", "软件服务",
                        "生鲜水果", "酒水饮料", "数码配件", "智能设备", "厨房用品", "清洁用品",
                        "家具建材", "其他商品"
                };
                // 预定义 100 个真实商品名称（对应 A-Y 类别的 4 个商品）
                String[] goodsNames = {
                        // A 电子产品
                        "iPhone 15 Pro", "华为 Mate 60", "小米 14 Ultra", "OPPO Find X7",
                        // B 服饰鞋帽
                        "优衣库 圆领T恤", "李宁 运动长裤", "耐克 Air Zoom", "阿迪达斯 连帽外套",
                        // C 食品生鲜
                        "三只松鼠 每日坚果", "良品铺子 猪肉脯", "伊利 金典纯牛奶", "五常 稻花香大米",
                        // D 家居用品
                        "宜家 收纳箱", "南极人 全棉四件套", "乐扣乐扣 水杯", "洁丽雅 纯棉毛巾",
                        // E 图书文娱
                        "《三体》全集", "《活着》余华", "晨光 中性笔套装", "得力 订书机",
                        // F 美妆个护
                        "兰蔻 小黑瓶", "雅诗兰黛 眼霜", "欧莱雅 洗发水", "舒肤佳 沐浴露",
                        // G 运动户外
                        "迪卡侬 瑜伽垫", "探路者 冲锋衣", "Keep 智能跳绳", "骆驼 登山鞋",
                        // H 玩具乐器
                        "乐高 机械组", "万代 高达模型", "雅马哈 电子琴", "费雪 益智积木",
                        // I 家用电器
                        "美的 变频空调", "海尔 双门冰箱", "格力 电风扇", "戴森 吸尘器",
                        // J 珠宝首饰
                        "周大福 黄金项链", "周生生 铂金戒指", "施华洛世奇 天鹅项链", "潘多拉 串饰手链",
                        // K 宠物用品
                        "皇家 猫粮", "麦富迪 狗粮", "小佩 智能饮水机", "pidan 猫砂盆",
                        // L 办公文具
                        "惠普 打印机", "得力 碎纸机", "齐心 文件柜", "晨光 笔记本",
                        // M 汽车用品
                        "米其林 轮胎", "3M 汽车贴膜", "70迈 行车记录仪", "博世 雨刮器",
                        // N 医药健康
                        "连花清瘟胶囊", "同仁堂 六味地黄丸", "鱼跃 电子血压计", "欧姆龙 血糖仪",
                        // O 母婴用品
                        "帮宝适 纸尿裤", "爱他美 奶粉", "好孩子 婴儿车", "贝亲 奶瓶",
                        // P 箱包皮具
                        "新秀丽 拉杆箱", "外交官 双肩包", "稻草人 钱包", "爱华仕 背包",
                        // Q 五金工具
                        "博世 电钻", "史丹利 扳手", "世达 螺丝刀套装", "牧田 角磨机",
                        // R 软件服务
                        "WPS 会员年卡", "Adobe 全家桶", "杀毒软件 三年版", "云存储 1TB",
                        // S 生鲜水果
                        "智利 车厘子", "泰国 金枕榴莲", "海南 贵妃芒", "新疆 阿克苏苹果",
                        // T 酒水饮料
                        "贵州茅台 飞天", "五粮液 普五", "农夫山泉 矿泉水", "可口可乐 汽水",
                        // U 数码配件
                        "安克 充电宝", "绿联 数据线", "闪迪 U盘", "罗技 无线鼠标",
                        // V 智能设备
                        "小爱同学 音箱", "天猫精灵 音箱", "小米 智能门锁", "华为 智能手环",
                        // W 厨房用品
                        "苏泊尔 炒锅", "双立人 刀具", "美的 电饭煲", "九阳 豆浆机",
                        // X 清洁用品
                        "蓝月亮 洗衣液", "威猛先生 洁厕灵", "滴露 消毒液", "心相印 抽纸",
                        // Y 家具建材
                        "林氏木业 沙发", "全友 双人床", "九牧 马桶", "欧普 照明灯"
                };
                for (int i = 0; i < 100; i++) {
                    // 1. 计算类别索引 (0-24，对应 A-Y)
                    int categoryIndex = i / 4;
                    char typeLetter = (char) ('A' + categoryIndex);

                    // 2. 计算该类别的序号 (1-4)
                    int serialNumber = i % 4 + 1;

                    // 3. 格式化编号：A0001, A0002, A0003, A0004, B0001...
                    String id = String.format("%c%04d", typeLetter, serialNumber);

                    // 4. 获取商品名称、类别
                    String categoryName = categories[categoryIndex];
                    String name = categoryName + " - " + goodsNames[i];


                    // 5. 生成制造厂商和型号
                    String factory = "厂商-" + typeLetter;
                    String model = typeLetter + "-Model-" + serialNumber;

                    // 6. 模拟真实价格与库存
                    double inPrice = 100.0 + (categoryIndex * 20) + (serialNumber * 10); // 进货价
                    double outPrice = Math.round(inPrice * 1.35 * 100.0) / 100.0;      // 零售价（加价35%并保留两位小数）
                    int stock = 50 + (serialNumber * 10);                                      // 库存 60-90

                    // 7. 创建商品对象并加入列表
                    Goods g = new Goods(id, name, factory, new Date(), model, inPrice, outPrice, stock);
                    goodsList.add(g);
                }

                System.out.println("首次启动,已加载100个默认商品");
            }
            if (!store.hasSavedData()) saveAllData();
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
                continue;
            }
            String confirm = readString("请再次输入新密码：");
            if (!newPW.equals(confirm)) {
                System.out.println("两次输入不一致,请重新输入");
                continue;
            }
            currentAdmin.setPassword(newPW);
            currentAdmin.setDefaultPassword(false);
            saveAllData(); // 立即保存
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
       saveAllData(); //立即保存
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
        saveAllData();
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
                        saveAllData();
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
                    if (goodsList.isEmpty()) System.out.println("暂无商品");
                    else goodsList.forEach(g -> System.out.println(g.toAdminString()));
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
        for (Goods g : goodsList){
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
        if (id.isEmpty() || !Double.isFinite(inPrice) || !Double.isFinite(outPrice)
                || inPrice < 0 || outPrice < 0 || stock < 0) {
            System.out.println("商品编号不能为空，价格和库存必须为有效的非负数");
            return;
        }
        Goods g = new Goods(id, name, factory, dom, model, inPrice, outPrice, stock);
        goodsList.add(g);
        saveAllData(); //立即保存
        System.out.println("商品添加成功！:" + g.toAdminString());
    }

    private static void updateGoods(){
        String id = readString("请输入要修改的商品编号：");
        Goods target = null;
        for (Goods g : goodsList){
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
        String factory = readString("厂家（" + target.getFactory() + "）：");
        String dateText = readString("生产日期（yyyy-MM-dd，回车保留原值）：");
        Date dom = target.getDOM();
        if (!dateText.isEmpty()) {
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                format.setLenient(false);
                dom = format.parse(dateText);
            } catch (java.text.ParseException e) {
                System.out.println("日期无效，修改取消");
                return;
            }
        }
        String model = readString("型号（" + target.getModel() + "）：");
        String inPriceStr = readString("进货价（" + target.getInPrice() + "）：");
        String outPriceStr = readString("零售价（" + target.getOutPrice() + "）：");
        String stockStr = readString("库存（" + target.getStock() + "）：");
        double inPrice, outPrice;
        int stock;
        try {
            inPrice = inPriceStr.isEmpty() ? target.getInPrice() : Double.parseDouble(inPriceStr);
            outPrice = outPriceStr.isEmpty() ? target.getOutPrice() : Double.parseDouble(outPriceStr);
            stock = stockStr.isEmpty() ? target.getStock() : Integer.parseInt(stockStr);
            if (!Double.isFinite(inPrice) || !Double.isFinite(outPrice) || inPrice < 0 || outPrice < 0 || stock < 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println("价格和库存必须为有效的非负数，修改取消");
            return;
        }
        if (!name.isEmpty()) target.setGoodsName(name);
        if (!factory.isEmpty()) target.setFactory(factory);
        target.setDOM(dom);
        if (!model.isEmpty()) target.setModel(model);
        target.setInPrice(inPrice);
        target.setOutPrice(outPrice);
        target.setStock(stock);
        saveAllData(); //立即保存
        System.out.println("商品信息更新成功");
    }


    private static void deleteGoods(){
        String id = readString("请输入要删除的商品编号：");
        Goods target = null;
        for (Goods g : goodsList){
            if (g.getGoodsID().equals(id)){
                target = g;
                break;
            }
        }
        if (target == null) {
            System.out.println("未找到商品");
            return;
        }
        if (confirm("确认删除商品" + target.getGoodsName() + "吗？此操作不可恢复！")){
            goodsList.remove(target);
            for (Customer c : customers) c.getCart().remove(target);
            saveAllData(); //立即保存
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
        for (Goods g : goodsList){
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
        saveAllData(); //立即保存
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
            saveAllData();
            currentCustomer = target;
            System.out.println("登录成功！欢迎" + target.getUserName());
        }else {
            target.setLoginTimes(target.getLoginTimes() + 1);
            if (target.getLoginTimes() >= 3){
                target.setLocked(true);
                saveAllData();
                System.out.println("连续三次密码错误，账户已被锁定！");
            }else {
                saveAllData();
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
        saveAllData();
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
        }
        String phone = readString("手机号（" + currentCustomer.getPhone() + "）：");
        if (!phone.isEmpty()) {
            if (!User.isPhoneRight(phone)) {
                System.out.println("手机号格式错误！应为11位数字，1开头，第二位3-9");
                return;
            }
        }
        String email = readString("邮箱（" + currentCustomer.getEmail() + "）：");
        if (!email.isEmpty()) {
            if (!User.isEmailRight(email)){
                System.out.println("邮箱格式错误！示例：user@medium.com");
                return;
            }
        }
        String changePW = readString("是否修改密码（是/否）（回车默认否）：");
        String pendingPassword = currentCustomer.getPassword();
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
            pendingPassword = newPW;
        }
        if (!username.isEmpty()) currentCustomer.setUserName(username);
        if (!phone.isEmpty()) currentCustomer.setPhone(phone);
        if (!email.isEmpty()) currentCustomer.setEmail(email);
        currentCustomer.setPassword(pendingPassword);
        saveAllData(); //立即保存
        System.out.println("个人信息修改完成");
    }


    private static void customerBrowseGoods(){
        if (currentCustomer == null){System.out.println("请先登录"); return;}
        System.out.println("\n---商品列表---");
        if (goodsList.isEmpty()){System.out.println("暂无商品"); return;}
        goodsList.forEach(System.out::println);
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
        for (Goods g : goodsList){
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
        if ((long) amount + currentCustomer.getCart().getOrDefault(target, 0) > target.getStock()){
            System.out.println("库存不足,当前库存数：" + target.getStock());
            return;
        }
        Map<Goods, Integer> cart = currentCustomer.getCart();
        cart.put(target, cart.getOrDefault(target, 0) + amount);
        saveAllData();
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
            saveAllData();
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
            saveAllData();
            System.out.println("商品已从购物车中移除");
        }else {
            if (newAmount > target.getStock()){
                System.out.println("库存不足，当前库存数：" + target.getStock());
                return;
            }
            cart.put(target, newAmount);
            saveAllData();
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
        currentCustomer.addConsumption(order.getTotalAmount());
        cart.clear();
        saveAllData(); //立即保存（库存、消费金额已变）
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

