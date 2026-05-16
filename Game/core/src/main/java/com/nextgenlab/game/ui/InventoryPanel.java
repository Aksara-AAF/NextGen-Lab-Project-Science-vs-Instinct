package com.nextgenlab.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.nextgenlab.game.crafting.Item;
import com.nextgenlab.game.crafting.Resource;
import com.nextgenlab.game.entity.Researcher;

import java.util.EnumMap;

public class InventoryPanel {

    private final Stage stage;
    private final Label resourceLabel;
    private final Label equippedLabel;
    private final BitmapFont font;
    private final Texture bgTex;
    private boolean open = false;

    public InventoryPanel() {
        font  = new BitmapFont();
        stage = new Stage(new ScreenViewport());

        Pixmap bg = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        bg.setColor(0f, 0f, 0f, 0.82f);
        bg.fill();
        bgTex = new Texture(bg);
        bg.dispose();

        Label.LabelStyle style = new Label.LabelStyle(font, Color.WHITE);
        resourceLabel = new Label("", style);
        resourceLabel.setFontScale(1.1f);
        equippedLabel = new Label("", style);
        equippedLabel.setFontScale(1.1f);

        Label.LabelStyle titleStyle = new Label.LabelStyle(font, Color.CYAN);
        Label title = new Label("INVENTORY  [I / ESC to close]", titleStyle);
        title.setFontScale(1.2f);

        TextureRegionDrawable bgDrawable = new TextureRegionDrawable(new TextureRegion(bgTex));

        Table root = new Table();
        root.setFillParent(true);
        root.setBackground(bgDrawable);
        root.pad(20);
        root.top().left();
        root.add(title).left().padBottom(12).row();
        root.add(resourceLabel).left().padBottom(8).row();
        root.add(equippedLabel).left().row();

        stage.addActor(root);
    }

    public void refresh(Researcher researcher) {
        EnumMap<com.nextgenlab.game.crafting.Resource, Integer> inv = researcher.getInventory();
        StringBuilder res = new StringBuilder("Resources:\n");
        for (Resource r : Resource.values()) {
            int count = inv.getOrDefault(r, 0);
            res.append("  ").append(r.name()).append(": ").append(count).append('\n');
        }
        resourceLabel.setText(res.toString());

        StringBuilder eq = new StringBuilder("Equipped:\n");
        Item w = researcher.getEquippedWeapon();
        Item u = researcher.getEquippedUtility();
        eq.append("  Weapon : ").append(w != null ? w.type.displayName : "—").append('\n');
        eq.append("  Utility: ").append(u != null ? u.type.displayName : "—").append('\n');
        equippedLabel.setText(eq.toString());
    }


    public boolean toggle(Researcher researcher) {
        open = !open;
        if (open) refresh(researcher);
        return open;
    }

    public boolean isOpen() { return open; }

    public boolean checkClose() {
        if (!open) return false;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.I)) {
            open = false;
            return true;
        }
        return false;
    }

    public Stage getStage() { return stage; }

    public void render() {
        if (!open) return;
        stage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    public void dispose() {
        stage.dispose();
        font.dispose();
        bgTex.dispose();
    }
}
