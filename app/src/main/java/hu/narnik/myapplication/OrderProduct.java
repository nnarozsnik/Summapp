package hu.narnik.myapplication;

public class OrderProduct {

    private String partner;
    private String product;
    private int quantity;
    private String date;
    private String id;

    public OrderProduct() {}

    private String author;

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public OrderProduct(String partner, String product, int quantity, String date) {
        this.partner = partner;
        this.product = product;
        this.quantity = quantity;
        this.date = date;

    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }


    public String getPartner() { return partner; }
    public int getQuantity() { return quantity; }
    public String getProduct() { return product; }
    public String getDate() { return date; }

    public void setPartner(String partner) { this.partner = partner; }
    public void setProduct(String product) { this.product = product; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setDate(String date) { this.date = date; }

}
