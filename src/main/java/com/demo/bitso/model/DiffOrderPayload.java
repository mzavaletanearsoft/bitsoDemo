package com.demo.bitso.model;

public class DiffOrderPayload {
    private String o;
    private String d;
    private String r;
    private Integer t;
    private String a;
    private String v;
    private String s;

    public String getO() {
        return o;
    }

    public void setO(String o) {
        this.o = o;
    }

    public String getD() {
        return d;
    }

    public void setD(String d) {
        this.d = d;
    }

    public String getR() {
        return r;
    }

    public void setR(String r) {
        this.r = r;
    }

    public Integer getT() {
        return t;
    }

    public void setT(Integer t) {
        this.t = t;
    }

    public String getA() {
        return a;
    }

    public void setA(String a) {
        this.a = a;
    }

    public String getV() {
        return v;
    }

    public void setV(String v) {
        this.v = v;
    }

    public String getS() {
        return s;
    }

    public void setS(String s) {
        this.s = s;
    }

    public double getRate() {
        return Double.valueOf(getR());
    }

}
