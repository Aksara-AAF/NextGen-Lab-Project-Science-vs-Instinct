package com.nextgenlab.game.network;

public class GameEvent {

    public static final String LEVEL_UP_START = "LEVEL_UP_START";
    public static final String LEVEL_UP_DONE  = "LEVEL_UP_DONE";
    public static final String SAB_LIGHTS     = "SAB_LIGHTS";
    public static final String SAB_SLOW       = "SAB_SLOW";
    public static final String SAB_DRAIN      = "SAB_DRAIN";
    public static final String TOXIC_SLOW     = "TOXIC_SLOW";

    public long   matchId;
    public String role;
    public String eventType;
    public String payload;
    public long   timestamp;
}
