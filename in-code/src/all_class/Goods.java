package all_class;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Goods {
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
