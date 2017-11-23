package com.demo.bitso.model;

public class TradesPayload implements Cloneable {
    private String book;
    private String created_at;
    private String amount;
    private String maker_side;
    private String price;
    private Integer tid;

    public String getBook() {
        return book;
    }

    public void setBook(String book) {
        this.book = book;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getMaker_side() {
        return maker_side;
    }

    public void setMaker_side(String maker_side) {
        this.maker_side = maker_side;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public Integer getTid() {
        return tid;
    }

    public void setTid(Integer tid) {
        this.tid = tid;
    }

    @Override
    public String toString() {
        String baseMessage = "[%s - %s] [AMOUNT: %s] [PRICE: %s] [TID: %s]";
        return String.format(baseMessage, getBook(), getMaker_side(), getAmount(), getPrice(), getTid());
    }

//    @Override
//    public int compareTo(Object o) {
//        return getTid() - ((TradesPayload) o).getTid();
//    }

    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public static TradesPayload clone(TradesPayload tradesPayload) {
        try {
            return (TradesPayload) tradesPayload.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        return new TradesPayload();

    }
}
