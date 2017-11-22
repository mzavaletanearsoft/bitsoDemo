package com.demo.bitso.model;

public class TradesPayload {
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
        String baseMessage = "[%s - %s] [AMOUNT: %s] [PRICE: %s]";
        return String.format(baseMessage, getBook(), getMaker_side(), getAmount(), getPrice());
    }
}
