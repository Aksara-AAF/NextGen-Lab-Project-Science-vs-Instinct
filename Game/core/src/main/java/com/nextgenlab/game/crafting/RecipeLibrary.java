package com.nextgenlab.game.crafting;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class RecipeLibrary {

    private static final List<Recipe> RECIPES = Collections.unmodifiableList(Arrays.asList(
        make(ItemType.PISTOL,       Resource.SCRAP,    2),
        make(ItemType.STUN_GUN,     Resource.BATTERY,  1, Resource.SCRAP,     1),
        make(ItemType.ACID_GRENADE, Resource.CHEMICAL, 2),
        make(ItemType.TASER,        Resource.BATTERY,  2),
        make(ItemType.RAIL_GUN,     Resource.BIOPLASMA,1, Resource.CIRCUIT,   2),
        make(ItemType.HEAL_KIT,     Resource.BIOPLASMA,1, Resource.CHEMICAL,  1),
        make(ItemType.TRAP,         Resource.SCRAP,    1, Resource.BATTERY,   1),
        make(ItemType.DECOY,        Resource.CIRCUIT,  1, Resource.CHEMICAL,  1)
    ));

    public static List<Recipe> getAll() { return RECIPES; }

    private static Recipe make(ItemType out, Object... pairs) {
        Map<Resource, Integer> map = new EnumMap<>(Resource.class);
        for (int i = 0; i < pairs.length; i += 2) {
            map.merge((Resource) pairs[i], (Integer) pairs[i + 1], Integer::sum);
        }
        return new Recipe(out, map);
    }
}
