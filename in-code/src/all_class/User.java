package all_class;

import java.text.SimpleDateFormat;
import java.util.*;

abstract class User {
    protected String userID;
    protected String userName;
    protected String password;
    protected String phone;
    protected String email;
    protected Date registerTime;
    protected double totalConsumption;
    protected int loginTimes;
    protected boolean locked;

    public User(String userName, String password, String phone, String email){
        this.userID = UUID.randomUUID().toString().substring(0,8);
        this.userName = userName;
        this.password = password;
        this.phone = phone;
        this.email = email;
        this.registerTime = new Date();
        this.totalConsumption = 0.00;
        this.loginTimes = 0;
        this.locked = false;

    }

    public String getUserID(){return userID;}
    public String getUserName(){return userName;}
    public void setUserName(String userName){this.userName = userName;}
    public String getPassword(){return password;}
    public void setPassword(String password){this.password = password;}
    public String getPhone(){return phone;}
    public void setPhone(String phone){this.phone = phone;}
    public String getEmail(){return email;}
    public void setEmail(String email){this.email = email;}
    public Date getRegisterTime(){return registerTime;}
    public double getTotalConsumption(){return totalConsumption;}
    public void addConsumption(double amount){this.totalConsumption += amount;}
    public int getLoginTimes(){return loginTimes;}
    public void setLoginTimes(int loginTimes){this.loginTimes = loginTimes;}
    public boolean isLocked(){return locked;}
    public void setLocked(boolean locked){this.locked = locked;}

    public boolean isPasswordSafe(String iPS){
        if (iPS.length() < 9) return false;
        boolean hasUpperCase = false, hasLowerCase = false , hasDigit = false, hasPunctuation = false;
        String punctuation = "[]{}`~!@#$%^&*()-_|''/?,.<>;:+-";
        for (char c : iPS.toCharArray()){
            if(Character.isUpperCase(c)) hasUpperCase = true;
            else if (Character.isLowerCase(c)) hasLowerCase = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (punctuation.indexOf(c) >= 0 ) hasPunctuation = true;

        }
        return hasUpperCase && hasLowerCase && hasDigit && hasPunctuation;
    }

    public static String randomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz`~!@#$%^&*()_+-={}[]|:;'/?,.<>";
        StringBuilder sb = new StringBuilder();
        Random rd = new Random();
        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(rd.nextInt(chars.length())));
        }
        return sb.toString();
    }
    @Override
    public String toString(){
        SimpleDateFormat sdf = new SimpleDateFormat("xxxx-xx-xx xx-xx-xx");
        return "ID：" + userID +  ", 用户名：" + userName + ", 手机：" + phone + ", 邮箱：" + email + ", 注册时间：" + sdf.format(registerTime) + ", 累计消费金额：" + totalConsumption;
    }

}

class Customer extends User{
    private Map<Goods, Integer> cart;
    private List<Order> orderHistory;

    public Customer(String userName, String password, String phone, String email){
        super(userName, password, phone, email);
        this.cart = new HashMap<>();
        this.orderHistory = new ArrayList<>();
    }

    public Map<Goods,Integer> getCart(){return cart;}
    public List<Order> getOrderHistory(){return orderHistory;}
    public void addOrder(Order order){orderHistory.add(order);}


    public String getLevel(){
        double amount = getTotalConsumption();
        if (amount >= 10000) return "金牌顾客";
        else if (amount >= 5000) return "银牌顾客";
        else return "铜牌顾客";


    }
    @Override
    public String toString(){
        return super.toString() + ", 级别：" + getLevel();
    }
}


