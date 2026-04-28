package com.nextgenlab.game.state;

import com.nextgenlab.game.screen.GameScreen;

public interface GameStateHandler {
    void enter(GameScreen screen);
    void update(float delta, GameScreen screen);
    void render(GameScreen screen);
    void exit(GameScreen screen);
    void dispose();
}
