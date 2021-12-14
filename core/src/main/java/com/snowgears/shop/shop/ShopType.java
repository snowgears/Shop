package com.snowgears.shop.shop;

public enum ShopType {

    SELL(0),

    BUY(1),

    BARTER(2),

    GAMBLE(3),

    COMBO(4);

    private final int slot;

    ShopType(int slot) {
        this.slot = slot;
    }

    @Override
    public String toString() {
        switch (this) {
            case SELL:
                return "sell";
            case BUY:
                return "buy";
            case BARTER:
                return "barter";
            case COMBO:
                return "combo";
            default:
                return "gamble";
        }
    }
}