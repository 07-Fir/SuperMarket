package all_class;

public class Administrators implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String account;
    private String password;
    // 保留旧字段和 serialVersionUID，false 表示从 2.0 反序列化的明文密码。
    private boolean passwordHashed;
    private boolean isDefaultPassword;

    public Administrators(String account, String password) {
        this(account, password, false);
    }

    private Administrators(String account, String password, boolean encoded) {
        this.account = account;
        this.password = encoded ? PasswordHash.requireEncoded(password) : PasswordHash.hash(password);
        this.passwordHashed = true;
        this.isDefaultPassword = true;
    }

    public String getAccount(){return  account;}
    public void setAccount(String account){this.account = account;}
    public String getPasswordHash(){return password;}
    public boolean verifyPassword(String input){return PasswordHash.verify(input, password);}
    public void setPassword(String password){this.password = PasswordHash.hash(password); this.passwordHashed = true;}
    static Administrators fromPasswordHash(String account, String hash) { return new Administrators(account, hash, true); }

    private void readObject(java.io.ObjectInputStream input) throws java.io.IOException, ClassNotFoundException {
        input.defaultReadObject();
        try {
            password = passwordHashed ? PasswordHash.requireEncoded(password) : PasswordHash.hash(password);
            passwordHashed = true;
        } catch (RuntimeException e) { throw new java.io.IOException("管理员密码数据无效", e); }
    }
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
            Administrators a = fromPasswordHash(parts[0], PasswordHash.importText(parts[1]));
            a.setDefaultPassword(Boolean.parseBoolean(parts[2]));
            return a;
        } catch (Exception e){
            return null;
        }
    }
}
