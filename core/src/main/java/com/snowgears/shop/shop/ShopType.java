package com.snowgears.shop.shop;

import com.snowgears.shop.util.ShopMessage;

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

    public String toCreationWord() {
        switch (this) {
            case SELL:
                return ShopMessage.getCreationWord("SELL");
            case BUY:
                return ShopMessage.getCreationWord("BUY");
            case BARTER:
                return ShopMessage.getCreationWord("BARTER");
            case COMBO:
                return ShopMessage.getCreationWord("COMBO");
            default:
                return ShopMessage.getCreationWord("GAMBLE");
        }
    }
}