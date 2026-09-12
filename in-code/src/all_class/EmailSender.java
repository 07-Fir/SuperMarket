package all_class;

public interface EmailSender {
    void sendEmail(String to, String content);
}
class MockEmailSender implements EmailSender{
    @Override
    public void sendEmail(String to, String content){
        System.out.println("模拟邮件发送至" + to);
        System.out.println("内容" + content);
        System.out.println("模拟发送成功");
    }
}