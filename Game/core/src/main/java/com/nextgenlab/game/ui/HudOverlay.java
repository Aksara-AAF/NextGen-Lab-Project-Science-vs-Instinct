package com.nextgenlab.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.nextgenlab.game.crafting.Item;
import com.nextgenlab.game.crafting.ItemType;

public class HudOverlay {

    private static final float SLOT_SIZE = 48f;
    private static final float SLOT_PAD  = 6f;

    private static final Color[] ITEM_FALLBACK_COLORS = {
        Color.YELLOW, Color.CYAN, Color.GREEN, new Color(1f, 0.5f, 0f, 1f),
        Color.MAGENTA, Color.RED, Color.GRAY, Color.BLUE
    };

    private final ShapeRenderer shapes;
    private final BitmapFont    font;
    private final SpriteBatch   hudBatch;
    private final OrthographicCamera hudCamera;
    private final StaminaBar    staminaBar;
    private final Texture[]     itemIcons = new Texture[ItemType.values().length];

    public HudOverlay() {
        shapes     = new ShapeRenderer();
        font       = new BitmapFont();
        hudBatch   = new SpriteBatch();
        hudCamera  = new OrthographicCamera();
        staminaBar = new StaminaBar();
        hudCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        loadItemIcons();
    }

    private void loadItemIcons() {
        ItemType[] types = ItemType.values();
        for (int i = 0; i < types.length; i++) {
            if (Gdx.files.internal(types[i].iconPath).exists()) {
                itemIcons[i] = new Texture(types[i].iconPath);
            } else {
                itemIcons[i] = solidTex(ITEM_FALLBACK_COLORS[i % ITEM_FALLBACK_COLORS.length]);
            }
        }
    }

    private static Texture solidTex(Color color) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fill();
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }


    public void renderWeaponSlots(Item weapon, Item utility) {
        int W = Gdx.graphics.getWidth();
        int H = Gdx.graphics.getHeight();
        float slotY = SLOT_PAD + 80f;
        float slot1X = W - 2 * (SLOT_SIZE + SLOT_PAD) - SLOT_PAD;
        float slot2X = W - (SLOT_SIZE + SLOT_PAD);

        hudCamera.update();
        shapes.setProjectionMatrix(hudCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        shapes.setColor(0.1f, 0.1f, 0.1f, 1f);
        shapes.rect(slot1X, slotY, SLOT_SIZE, SLOT_SIZE);
        shapes.rect(slot2X, slotY, SLOT_SIZE, SLOT_SIZE);
        shapes.setColor(0.4f, 0.4f, 0.4f, 1f);
        shapes.rectLine(slot1X, slotY, slot1X + SLOT_SIZE, slotY, 1f);
        shapes.rectLine(slot1X, slotY + SLOT_SIZE, slot1X + SLOT_SIZE, slotY + SLOT_SIZE, 1f);
        shapes.end();

        hudBatch.setProjectionMatrix(hudCamera.combined);
        hudBatch.begin();
        if (weapon != null && !weapon.consumed) {
            Texture icon = itemIcons[weapon.type.ordinal()];
            hudBatch.draw(icon, slot1X + 4, slotY + 4, SLOT_SIZE - 8, SLOT_SIZE - 8);
        }
        if (utility != null && !utility.consumed) {
            Texture icon = itemIcons[utility.type.ordinal()];
            hudBatch.draw(icon, slot2X + 4, slotY + 4, SLOT_SIZE - 8, SLOT_SIZE - 8);
        }
        font.setColor(Color.LIGHT_GRAY);
        font.draw(hudBatch, "LMB", slot1X + 8, slotY - 2);
        font.draw(hudBatch, "F", slot2X + 18, slotY - 2);
        hudBatch.end();
    }

    public void renderPreparation(int tasksCompleted, int totalTasks,
                                  int resHp, int resMaxHp,
                                  float stamina, float maxStamina,
                                  int monHp, int monMaxHp) {
        int W = Gdx.graphics.getWidth();
        hudCamera.update();

        shapes.setProjectionMatrix(hudCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);


        shapes.setColor(0.15f, 0.15f, 0.15f, 1f);
        shapes.rect(10, Gdx.graphics.getHeight() - 34, 200, 18);
        float fill = totalTasks > 0 ? (float) tasksCompleted / totalTasks : 0f;
        shapes.setColor(Color.GREEN);
        shapes.rect(10, Gdx.graphics.getHeight() - 34, 200 * fill, 18);


        shapes.setColor(0.15f, 0.15f, 0.15f, 1f);
        shapes.rect(10, 10, 160, 16);
        shapes.setColor(Color.CYAN);
        shapes.rect(10, 10, resMaxHp > 0 ? 160f * resHp / resMaxHp : 0, 16);


        staminaBar.render(shapes, 10, 30, 160, 10, stamina, maxStamina);


        float barX = W - 170f;
        shapes.setColor(0.15f, 0.15f, 0.15f, 1f);
        shapes.rect(barX, 10, 160, 16);
        shapes.setColor(Color.RED);
        shapes.rect(barX, 10, monMaxHp > 0 ? 160f * monHp / monMaxHp : 0, 16);

        shapes.end();

        hudBatch.setProjectionMatrix(hudCamera.combined);
        hudBatch.begin();
        font.setColor(Color.WHITE);
        font.draw(hudBatch,
            "PERSIAPAN  " + tasksCompleted + "/" + totalTasks + " task selesai",
            10, Gdx.graphics.getHeight() - 10);
        font.draw(hudBatch, "[E] interaksi  [RMB] tembak  [Shift] sprint",
            10, Gdx.graphics.getHeight() - 44);
        font.draw(hudBatch, "Peneliti  " + resHp + "/" + resMaxHp, 10, 44);
        staminaBar.renderLabel(hudBatch, font, 10, 62, stamina, maxStamina);
        font.draw(hudBatch, "Monster  " + monHp + "/" + monMaxHp, barX, 44);
        hudBatch.end();
    }

    public void renderDuel(int resHp, int resMaxHp,
                           float stamina, float maxStamina,
                           int monHp, int monMaxHp) {
        int W = Gdx.graphics.getWidth();
        hudCamera.update();

        shapes.setProjectionMatrix(hudCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);


        shapes.setColor(0.15f, 0.15f, 0.15f, 1f);
        shapes.rect(10, 10, 160, 16);
        shapes.setColor(Color.CYAN);
        shapes.rect(10, 10, resMaxHp > 0 ? 160f * resHp / resMaxHp : 0, 16);


        staminaBar.render(shapes, 10, 30, 160, 10, stamina, maxStamina);


        float barX = W - 170f;
        shapes.setColor(0.15f, 0.15f, 0.15f, 1f);
        shapes.rect(barX, 10, 160, 16);
        shapes.setColor(Color.RED);
        shapes.rect(barX, 10, monMaxHp > 0 ? 160f * monHp / monMaxHp : 0, 16);

        shapes.end();

        hudBatch.setProjectionMatrix(hudCamera.combined);
        hudBatch.begin();
        font.setColor(Color.WHITE);
        font.draw(hudBatch, "FASE: DUEL", W / 2f - 36, Gdx.graphics.getHeight() - 8);
        font.draw(hudBatch, "Peneliti  " + resHp + "/" + resMaxHp, 10, 44);
        staminaBar.renderLabel(hudBatch, font, 10, 62, stamina, maxStamina);
        font.draw(hudBatch, "Monster  "  + monHp + "/" + monMaxHp, barX, 44);
        font.draw(hudBatch, "[RMB] tembak  [LMB] serang  [Shift] sprint", W / 2f - 90, 44);
        hudBatch.end();
    }

    public void dispose() {
        shapes.dispose();
        font.dispose();
        hudBatch.dispose();
        for (Texture t : itemIcons) { if (t != null) t.dispose(); }
    }
}
