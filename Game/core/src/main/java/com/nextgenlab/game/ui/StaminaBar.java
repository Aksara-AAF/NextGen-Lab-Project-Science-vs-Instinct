package com.nextgenlab.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class StaminaBar {


    public void render(ShapeRenderer shapes, float x, float y, float w, float h,
                       float stamina, float maxStamina) {
        shapes.setColor(0.15f, 0.15f, 0.15f, 1f);
        shapes.rect(x, y, w, h);
        float fill = maxStamina > 0 ? stamina / maxStamina : 0f;
        shapes.setColor(1f, 0.85f, 0f, 1f);
        shapes.rect(x, y, w * fill, h);
    }


    public void renderLabel(SpriteBatch batch, BitmapFont font, float x, float y,
                            float stamina, float maxStamina) {
        font.setColor(Color.YELLOW);
        font.draw(batch, "Stamina  " + (int) stamina + "/" + (int) maxStamina, x, y);
        font.setColor(Color.WHITE);
    }
}
