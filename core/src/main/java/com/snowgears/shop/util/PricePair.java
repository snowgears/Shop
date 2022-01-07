package com.snowgears.shop.util;

public class PricePair {

    private double price;
    private double priceCombo;

    public PricePair(double price, double priceCombo){
        this.price = price;
        this.priceCombo = priceCombo;
    }

    public double getPrice() {
        return price;
    }

    public double getPriceCombo() {
        return priceCombo;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setPriceCombo(double priceCombo) {
        this.priceCombo = priceCombo;
    }
}
