package com.nextgenlab.game.factory;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.nextgenlab.game.entity.Chest;
import com.nextgenlab.game.entity.Guard;
import com.nextgenlab.game.entity.Monster;
import com.nextgenlab.game.entity.Researcher;
import com.nextgenlab.game.entity.SabotagePanel;

import java.util.Random;

public class EntityFactory {

    public static Researcher createResearcher(float x, float y, TiledMap map) {
        return new Researcher(x, y, map);
    }

    public static Monster createMonster(float x, float y, TiledMap map) {
        return new Monster(x, y, map);
    }

    public static Guard createGuard(float x, float y, TiledMap map) {
        return new Guard(x, y, map);
    }

    public static Chest createChest(float x, float y, Random rng) {
        return new Chest(x, y, rng);
    }

    public static SabotagePanel createSabotagePanel(float x, float y, SabotagePanel.Type type) {
        return new SabotagePanel(x, y, type);
    }
}
