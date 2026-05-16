package com.nextgenlab.game.crafting;

import java.util.Random;

public enum Resource {
    SCRAP(35), BATTERY(25), CHEMICAL(20), CIRCUIT(15), BIOPLASMA(5);

    public final int weight;

    Resource(int weight) { this.weight = weight; }

    public static Resource weightedRandom(Random rng) {
        int total = 0;
        for (Resource r : values()) total += r.weight;
        int roll = rng.nextInt(total);
        for (Resource r : values()) {
            roll -= r.weight;
            if (roll < 0) return r;
        }
        return SCRAP;
    }
}
