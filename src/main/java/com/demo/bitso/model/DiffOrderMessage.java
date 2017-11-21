package com.demo.bitso.model;

import java.util.List;

public class DiffOrderMessage {
    private String type;
    private String book;
    private Integer sequence;
    private List<Payload> payload;


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBook() {
        return book;
    }

    public void setBook(String book) {
        this.book = book;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public List<Payload> getPayload() {
        return payload;
    }

    public void setPayload(List<Payload> payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        Payload payload = getFirstPayload();
        String operation = isBuy() ? "BUY" : "SELL";
        String rate = payload.getR();
        String amount = payload.getA();
        String value = payload.getV();
        String orderId = payload.getO();
        Integer sequence = getSequence();

        String baseMessage = "[%s] [RATE: %s] [AMOUNT: %s] [VALUE: %s] [ID: %s] [SEQ: %s]";
        return String.format(baseMessage, operation, rate, amount, value, orderId, sequence);
    }

    public Payload getFirstPayload() {
        return getPayload().get(0);
    }

    public boolean isBuy() {
        return getFirstPayload().getT() == 0;
    }
}
