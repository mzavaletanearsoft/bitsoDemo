package com.demo.bitso.model;

import java.util.List;

public class DiffOrderMessage implements Cloneable {
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

        String book = formatValue(getBook());
        String operation = formatValue(isBuy() ? "BUY" : "SELL");
        String rate = formatValue(payload.getR());
        String amount = formatValue(payload.getA());
        String value = formatValue(payload.getV());
        String operationId = formatValue(payload.getO());
        String status = formatValue(payload.getS());
        Integer sequence = getSequence();

        String baseMessage = "[%s - %s] [RATE: %s] [AMOUNT: %s] [VALUE: %s] [ID: %s] [STATUS: %s] [SEQ: %s]";
        return String.format(baseMessage, book, operation, rate, amount, value, operationId, status, sequence);
    }

    private String formatValue(String value) {
        return value == null ? "-" : value;
    }

    public Payload getFirstPayload() {
        return getPayload().get(0);
    }

    public boolean isBuy() {
        return getFirstPayload().getT() == 0;
    }

    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public static DiffOrderMessage clone(DiffOrderMessage diffOrderMessage) {
        try {
            return (DiffOrderMessage) diffOrderMessage.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        return null;
    }
}
