package com.nextgenlab.game.command;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.nextgenlab.game.entity.Direction;

public class InputHandler {

    public MoveCommand resolve() {
        boolean w = Gdx.input.isKeyPressed(Input.Keys.W);
        boolean s = Gdx.input.isKeyPressed(Input.Keys.S);
        boolean a = Gdx.input.isKeyPressed(Input.Keys.A);
        boolean d = Gdx.input.isKeyPressed(Input.Keys.D);

        if (!w && !s && !a && !d) {
            return (researcher, delta) -> researcher.applyIdle();
        }

        float vx = 0, vy = 0;
        Direction dir;

        if (w && d) {
            vy =  1;
            vx =  1;
            dir = Direction.NE;
        }
        else if (w && a) {
            vy =  1;
            vx = -1;
            dir = Direction.NW;
        }
        else if (s && d) {
            vy = -1;
            vx =  1;
            dir = Direction.SE;
        }
        else if (s && a) {
            vy = -1;
            vx = -1;
            dir = Direction.SW;
        }
        else if (w) {
            vy =  1;
            dir = Direction.N;
        }
        else if (s) {
            vy = -1;
            dir = Direction.S;
        }
        else if (d) {
            vx =  1;
            dir = Direction.E;
        }
        else {
            vx = -1;
            dir = Direction.W;
        }

        final float fvx = vx, fvy = vy;
        final Direction fdir = dir;
        return (researcher, delta) -> researcher.applyMovement(fvx, fvy, fdir, delta);
    }

    public boolean isShootPressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
    }

    public boolean isInteractPressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.E);
    }
}
