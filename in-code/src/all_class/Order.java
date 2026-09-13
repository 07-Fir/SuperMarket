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
        this(customer, goods, conutMap, paymentMethod, true);
    }

    private Order(Customer customer, List<Goods> goods, Map<String, Integer> conutMap, String paymentMethod, boolean fresh){
        this.orderID = fresh ? ++counter : 0;
        this.customer = customer;
        // 复制购买时的商品信息，以后改价不会改变历史订单。
        this.goods = new ArrayList<>();
        for (Goods g : goods) {
            this.goods.add(new Goods(g.getGoodsID(), g.getGoodsName(), g.getFactory(),
                    g.getDOM() == null ? null : new Date(g.getDOM().getTime()), g.getModel(),
                    g.getInPrice(), g.getOutPrice(), g.getStock()));
        }
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

    public String toFileLine() {
        // 前六项：订单号、顾客ID、支付方式、时间、总额、商品种数。
        List<Object> fields = new ArrayList<>(Arrays.asList(orderID, customer.getUserID(),
                paymentMethod, buyTime.getTime(), totalAmount, goods.size()));
        for (Goods g : goods) {
            // 每种商品再保存两项：商品信息、购买数量。
            fields.add(g.toFileLine());
            fields.add(goodsCountMap.get(g.getGoodsID()));
        }
        return TextCodec.join(fields.toArray());
    }

    public static Order fromFileLine(String line, Map<String, Customer> customers) {
        // 按上面的顺序读取。恢复订单时，不重复增加顾客的消费金额。
        String[] p = TextCodec.split(line);
        if (p.length < 6) throw new IllegalArgumentException("订单字段不足");
        int id = Integer.parseInt(p[0]);
        Customer customer = customers.get(p[1]);
        int size = Integer.parseInt(p[5]);
        if (id <= 0 || customer == null || size <= 0 || size > (p.length - 6) / 2 || p.length != 6 + size * 2)
            throw new IllegalArgumentException("订单数据无效");
        List<Goods> items = new ArrayList<>();
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < size; i++) {
            Goods g = Goods.fromFileLine(p[6 + i * 2]);
            int count = Integer.parseInt(p[7 + i * 2]);
            if (g == null || count <= 0 || counts.put(g.getGoodsID(), count) != null)
                throw new IllegalArgumentException("订单明细无效");
            items.add(g);
        }
        Order order = new Order(customer, items, counts, p[2], false);
        double total = Double.parseDouble(p[4]);
        if (!Double.isFinite(total) || Math.abs(total - order.totalAmount) > 0.000001)
            throw new IllegalArgumentException("订单金额无效");
        order.orderID = id;
        counter = Math.max(counter, id);
        order.buyTime = new Date(Long.parseLong(p[3]));
        order.totalAmount = total;
        return order;
    }

    public String getPay(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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
