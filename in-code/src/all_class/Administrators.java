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


}
