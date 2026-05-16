package com.nextgenlab.game.network;

public class PositionUpdate {
    public static final int FLAG_DASHING     = 1 << 0;
    public static final int FLAG_ATTACKING   = 1 << 1;
    public static final int FLAG_HIT         = 1 << 2;
    public static final int FLAG_SABOTAGING  = 1 << 3;
    public static final int FLAG_INTERACTING = 1 << 4;

    public long    matchId;
    public String  role;
    public float   x, y;
    public int     direction;
    public boolean moving;
    public int     actionFlag;
    public int     hp;
    public int     taskProgress;
    public long    timestamp;


    public int guardAliveMask;
    public int guardRespawnEvent;


    public int equippedItemOrdinal = -1;


    public int maxHp = 5;


    public int monsterXp    = 0;
    public int monsterLevel = 0;
}
