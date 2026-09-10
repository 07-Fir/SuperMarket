package all_class;

import java.text.SimpleDateFormat;
import java.util.*;

public class Order {
    private static int counter = 1000;
    private int orderID;
    private Customer customer;
    private List<Goods> goods;
    private Map<String, Integer> goodsCountMap;
    double totalAmount;
    private String paymentMethod;
    private Date buyTime;

    public Order(Customer customer, List<Goods> goods, Map<String, Integer> conutMap, String paymentMethod){
        this.orderID = ++counter;
        this.customer = customer;
        this.goods = new ArrayList<>(goods);
        this.goodsCountMap = new HashMap<>(conutMap);
        this.paymentMethod = paymentMethod;
        this.buyTime = new Date();
        this.totalAmount = 0;
        for (Goods g : goods){
            int cnt = conutMap.getOrDefault(g.getGoodsID(),0);
            this.totalAmount += g.getOutPrice()*cnt;
        }

    }

    public int getOrderID(){return orderID;}
    public Customer getCustomer(){return customer;}
    public List<Goods> getGoods(){return goods;}
    public Map<String,Integer> getGoodsCountMap(){return goodsCountMap;}
    public double getTotalAmount(){return totalAmount;}
    public String getPaymentMethod(){return paymentMethod;}
    public Date getBuyTime(){return buyTime;}

    public String getPay(){
        SimpleDateFormat sdf = new SimpleDateFormat("xxxx-xx-xx xx:xx:xx");
        StringBuilder sb = new StringBuilder();
        sb.append("订单号：").append(orderID).append(", 顾客：").append(customer.getUserName())
                .append(", 支付方式：").append(paymentMethod)
                .append(", 总额：").append(totalAmount)
                .append(", 时间：").append(sdf.format(buyTime)).append("\n");
        sb.append("商品明细：\n");
        for (Goods g : goods){
            int cnt = goodsCountMap.get(g.getGoodsID());
            sb.append("  ").append(g.getGoodsName()).append(" x").append(cnt)
                    .append(" 单价：").append(g.getOutPrice())
                    .append(" 小计：").append(g.getOutPrice()*cnt).append("\n");
        }
        return sb.toString();

    }


}
