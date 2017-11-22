package com.demo.bitso.model;

import java.util.List;

public class TradesMessage {
    private Boolean success;
    private List<TradesPayload> payload;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public List<TradesPayload> getPayload() {
        return payload;
    }

    public void setPayload(List<TradesPayload> payload) {
        this.payload = payload;
    }
}
