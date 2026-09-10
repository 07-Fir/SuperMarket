package all_class;

public class Administrators {
    private String account;
    private String password;

    public Administrators(String account, String password) {
        this.account = account;
        this.password = password;
    }

    public String getAccount(){return  account;}
    public void setAccount(String account){this.account = account;}
    public String getPassword(){return password;}
    public void setPassword(String password){this.password = password;}


}
