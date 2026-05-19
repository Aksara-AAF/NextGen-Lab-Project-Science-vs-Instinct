package com.nextgenlab.game.event;

public class OnPlayerHit {
    public final String role;
    public final int damage;
    public OnPlayerHit(String role, int damage) { this.role = role; this.damage = damage; }
}
