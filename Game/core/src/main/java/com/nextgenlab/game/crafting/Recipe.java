package com.nextgenlab.game.crafting;

import java.util.Collections;
import java.util.Map;

public class Recipe {
    public final ItemType output;
    public final Map<Resource, Integer> ingredients;

    public Recipe(ItemType output, Map<Resource, Integer> ingredients) {
        this.output      = output;
        this.ingredients = Collections.unmodifiableMap(ingredients);
    }

    public boolean canCraft(Map<Resource, Integer> inventory) {
        for (Map.Entry<Resource, Integer> e : ingredients.entrySet()) {
            if (inventory.getOrDefault(e.getKey(), 0) < e.getValue()) return false;
        }
        return true;
    }
}
