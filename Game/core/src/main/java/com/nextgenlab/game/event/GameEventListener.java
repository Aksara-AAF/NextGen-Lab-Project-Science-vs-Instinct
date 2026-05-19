package com.nextgenlab.game.event;

@FunctionalInterface
public interface GameEventListener<E> {
    void onEvent(E event);
}
