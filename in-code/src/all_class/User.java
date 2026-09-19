package all_class;

import java.text.SimpleDateFormat;
import java.util.*;

abstract class User implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected String userID;
    protected String userName;
    protected String password;
    private boolean passwordHashed;
    protected String phone;
    protected String email;
    protected Date registerTime;
    protected double totalConsumption;
    protected int loginTimes;
    protected boolean locked;

    public User(String userName, String password, String phone, String email){
        this(userName, password, phone, email, false);
    }

    protected User(String userName, String password, String phone, String email, boolean encoded){
        this.userID = UUID.randomUUID().toString().substring(0,8);
        this.userName = userName;
        this.password = encoded ? PasswordHash.requireEncoded(password) : PasswordHash.hash(password);
        this.passwordHashed = true;
        this.phone = phone;
        this.email = email;
        this.registerTime = new Date();
        this.totalConsumption = 0.00;
        this.loginTimes = 0;
        this.locked = false;

    }

    public String getUserID(){return userID;}
    public void setUserID(String userID) {this.userID = userID;}
    public String getUserName(){return userName;}
    public void setUserName(String userName){this.userName = userName;}
    public String getPasswordHash(){return password;}
    public boolean verifyPassword(String input){return PasswordHash.verify(input, password);}
    public void setPassword(String password){this.password = PasswordHash.hash(password); this.passwordHashed = true;}

    private void readObject(java.io.ObjectInputStream input) throws java.io.IOException, ClassNotFoundException {
        input.defaultReadObject();
        try {
            password = passwordHashed ? PasswordHash.requireEncoded(password) : PasswordHash.hash(password);
            passwordHashed = true;
        } catch (RuntimeException e) { throw new java.io.IOException("用户密码数据无效", e); }
    }
    public String getPhone(){return phone;}
    public void setPhone(String phone){this.phone = phone;}
    public String getEmail(){return email;}
    public void setEmail(String email){this.email = email;}
    public Date getRegisterTime(){return registerTime;}
    public void setRegisterTime(Date registerTime) {this.registerTime = registerTime;}
    public double getTotalConsumption(){return totalConsumption;}
    public void setTotalConsumption(double totalConsumption){this.totalConsumption = totalConsumption;}
    public void addConsumption(double amount){this.totalConsumption += amount;}
    public int getLoginTimes(){return loginTimes;}
    public void setLoginTimes(int loginTimes){this.loginTimes = loginTimes;}
    public boolean isLocked(){return locked;}
    public void setLocked(boolean locked){this.locked = locked;}

    public static boolean isPasswordSafe(String iPS){
        if (iPS.length() < 9) return false;
        boolean hasUpperCase = false, hasLowerCase = false , hasDigit = false, hasPunctuation = false;
        String punctuation = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
        for (char c : iPS.toCharArray()){
            if(Character.isUpperCase(c)) hasUpperCase = true;
            else if (Character.isLowerCase(c)) hasLowerCase = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (punctuation.indexOf(c) >= 0 ) hasPunctuation = true;

        }
        return hasUpperCase && hasLowerCase && hasDigit && hasPunctuation;
    }

 // 使用正则表达式检验手机号格式
    public static boolean isPhoneRight(String phone){
        if (phone == null) return false;
        return phone.matches("^1[3-9]\\d{9}$");
    }


 // 使用正则表达式检验邮箱格式
    public static boolean isEmailRight(String email){
        if (email == null) return false;
        return email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
    }


    public static String randomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789`~!@#$%^&*()_+-={}[]|:;'/?,.<>";
        StringBuilder sb = new StringBuilder();
        Random rd = new java.security.SecureRandom();
        do {
            sb.setLength(0);
            for (int i = 0; i < 12; i++) sb.append(chars.charAt(rd.nextInt(chars.length())));
        } while (!isPasswordSafe(sb.toString()));
        return sb.toString();
    }
    @Override
    public String toString(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        return "ID：" + userID +  ", 用户名：" + userName + ", 手机：" + phone + ", 邮箱：" + email + ", 注册时间：" + sdf.format(registerTime) + ", 累计消费金额：" + totalConsumption;
    }

}

class Customer extends User{
    private static final long serialVersionUID = 1L;
    private Map<Goods, Integer> cart;
    private List<Order> orderHistory;

    public Customer(String userName, String password, String phone, String email){
        this(userName, password, phone, email, false);
    }

    private Customer(String userName, String password, String phone, String email, boolean encoded){
        super(userName, password, phone, email, encoded);
        this.cart = new HashMap<>();
        this.orderHistory = new ArrayList<>();
    }

    public Map<Goods,Integer> getCart(){return cart;}
    static Customer fromPasswordHash(String userName, String hash, String phone, String email) {
        return new Customer(userName, hash, phone, email, true);
    }
    public List<Order> getOrderHistory(){return orderHistory;}
    public void addOrder(Order order){orderHistory.add(order);}


    public String getLevel(){
        double amount = getTotalConsumption();
        if (amount >= 10000) return "金牌顾客";
        else if (amount >= 5000) return "银牌顾客";
        else return "铜牌顾客";
    }
    public String toFileLine(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return TextCodec.join(userID, userName, password, phone, email, sdf.format(registerTime), totalConsumption, loginTimes, locked);
    }


    public static Customer fromFileLine(String line){
        try {
            String[] parts = TextCodec.split(line);
            if (parts.length != 7 && parts.length != 9) return null;
            String userID = parts[0];
            String userName = parts[1];
            String password = parts[2];
            String phone = parts[3];
            String email = parts[4];
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setLenient(false);
            Date registerTime = sdf.parse(parts[5]);
            Double total = Double.parseDouble(parts[6]);

            Customer c = Customer.fromPasswordHash(userName, PasswordHash.importText(password), phone, email);
            c.setUserID(userID);
            c.setRegisterTime(registerTime);
            c.setTotalConsumption(total);
            if (!Double.isFinite(total) || total < 0) return null;
            if (parts.length == 9) {
                c.setLoginTimes(Integer.parseInt(parts[7]));
                if (c.getLoginTimes() < 0 || !(parts[8].equals("true") || parts[8].equals("false"))) return null;
                c.setLocked(Boolean.parseBoolean(parts[8]));
            }
            return c;
        } catch (Exception e){
            return null;
        }
    }

    @Override
    public String toString(){
        return super.toString() + ", 级别：" + getLevel();
    }
}


