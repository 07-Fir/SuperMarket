package all_class;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Goods implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String goodsID;
    private String goodsName;
    private String factory;
    private Date DOM;
    private String model;
    private double inPrice;
    private double outPrice;
    private int stock;

    public Goods(String goodsID, String goodsName, String factory, Date DOM, String model, double inPrice, double outPrice, int stock){
        this.goodsID = goodsID;
        this.goodsName = goodsName;
        this.factory = factory;
        this.DOM = DOM;
        this.model = model;
        this.inPrice = inPrice;
        this.outPrice = outPrice;
        this.stock = stock;

    }

    public String getGoodsID() { return goodsID; }
    public void setGoodsID(String goodsID) { this.goodsID = goodsID; }
    public String getGoodsName() { return goodsName; }
    public void setGoodsName(String goodsName) { this.goodsName = goodsName; }
    public String getFactory() { return factory; }
    public void setFactory(String factory) { this.factory = factory; }
    public Date getDOM() { return DOM; }
    public void setDOM(Date DOM) { this.DOM = DOM; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public double getInPrice() { return inPrice; }
    public void setInPrice(double inPrice) { this.inPrice = inPrice; }
    public double getOutPrice() { return outPrice; }
    public void setOutPrice(double outPrice) { this.outPrice = outPrice; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }


// 将对象转化为一行文本
    public String toFileLine(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = (DOM == null)?"": sdf.format(DOM);
        return TextCodec.join(goodsID, goodsName, factory, dateStr, model, inPrice, outPrice, stock);
    }


// 从一行文本中解析中商品对象
    public static Goods fromFileLine(String line){
        try {
            String[] parts = TextCodec.split(line);
            if (parts.length != 8) return null;
            String id = parts[0];
            String name = parts[1];
            String factory = parts[2];
            Date date = null;

            if (!parts[3].isEmpty()){
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.setLenient(false);
                date = sdf.parse(parts[3]);
            }
            String model = parts[4];
            double inPrice = Double.parseDouble(parts[5]);
            double outPrice = Double.parseDouble(parts[6]);
            int stock = Integer.parseInt(parts[7]);
            if (!Double.isFinite(inPrice) || !Double.isFinite(outPrice) || inPrice < 0 || outPrice < 0 || stock < 0) return null;
            return new Goods(id, name, factory, date, model, inPrice, outPrice, stock);
        } catch (Exception e){
            return null;
        }
    }




    @Override
    public String toString(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return "编号:" + goodsID + ", 名称:" + goodsName + ", 厂家:" + factory + ", 生产日期:" + (DOM == null?"未知":sdf.format(DOM)) + ", 型号:" + model + ", 售价:" + outPrice + ", 库存:" + stock;
    }

    public String toAdminString(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return "编号:" + goodsID + ", 名称:" + goodsName + ", 厂家:" + factory + ", 生产日期:" + (DOM == null?"未知":sdf.format(DOM)) + ", 型号:" + model + ", 进价:" + inPrice + ", 零售价:" + outPrice + ", 库存:" + stock;
    }
}
