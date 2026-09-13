package all_class;

public class Administrators {
    private String account;
    private String password;
    private boolean isDefaultPassword;

    public Administrators(String account, String password) {
        this.account = account;
        this.password = password;
        this.isDefaultPassword = true;
    }

    public String getAccount(){return  account;}
    public void setAccount(String account){this.account = account;}
    public String getPassword(){return password;}
    public void setPassword(String password){this.password = password;}
    public boolean isDefaultPassword() {return isDefaultPassword;}
    public void setDefaultPassword(boolean defaultPassword) {this.isDefaultPassword = defaultPassword;}



// 将管理员信息转为一行文本
    public String toFileLine(){
        return TextCodec.join(account, password, isDefaultPassword);
    }


// 将一行文本转为管理员信息
    public static Administrators fromFileLine(String line){
        try {
            String[] parts = TextCodec.split(line);
            if (parts.length != 3 || !(parts[2].equals("true") || parts[2].equals("false"))) return null;
            Administrators a = new Administrators(parts[0], parts[1]);
            a.setDefaultPassword(Boolean.parseBoolean(parts[2]));
            return a;
        } catch (Exception e){
            return null;
        }
    }
}
