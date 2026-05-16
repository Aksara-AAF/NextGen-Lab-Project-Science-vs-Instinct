package com.nextgenlab.game.strategy;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.nextgenlab.game.entity.Guard;

public interface GuardStrategy {
    void update(Guard guard, TiledMap map, float monsterX, float monsterY, float delta);
}
