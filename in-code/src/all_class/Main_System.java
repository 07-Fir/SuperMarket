package all_class;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class Main_System {
    private static Scanner sc = new Scanner(System.in);
    private static List<Administrators> admins = new ArrayList<>();
    private static List<Customer> customers = new ArrayList<>();
    private static List<Goods> goods = new ArrayList<>();
    private static Administrators currentAdmin = null;
    private static Customer currentCustomer = null;


    public static void main(String[] args){
        admins.add(new Administrators("admin","ynuinfo#777"));

        goods.add(new Goods("0001","智能手机", "华为",new Date(), "mate60", 2400.0, 2900.0, 50));
        goods.add(new Goods("0002","蓝牙耳机", "小米",new Date(), "Air2", 120.0, 199.0, 100));
        goods.add(new Goods("0003","笔记本电脑", "联想", new Date(), "ThinkPad X1", 4500.0, 5999.0, 20 ));

        System.out.println("=========== 欢迎使用购物管理系统 ===========");

    }
    private static void showMainMenu() {
        System.out.println("\n---------------主菜单---------------");
        System.out.println("1.管理员登录");
        System.out.println("2.顾客注册");
        System.out.println("3.顾客登录");

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
}


