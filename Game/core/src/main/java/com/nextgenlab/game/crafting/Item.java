package com.nextgenlab.game.crafting;

public class Item {
    public final ItemType type;
    public boolean consumed = false;

    public Item(ItemType type) {
        this.type = type;
    }
}
