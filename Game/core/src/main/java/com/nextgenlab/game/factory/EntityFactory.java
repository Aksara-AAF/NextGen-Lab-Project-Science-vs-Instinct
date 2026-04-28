package com.nextgenlab.game.factory;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.nextgenlab.game.entity.Monster;
import com.nextgenlab.game.entity.Researcher;

public class EntityFactory {

    public static Researcher createResearcher(TiledMap map) {
        return new Researcher(200, 200, map);
    }

    public static Monster createMonster(TiledMap map) {
        return new Monster(700, 700, map);
    }
}
