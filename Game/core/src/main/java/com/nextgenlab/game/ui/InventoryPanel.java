package com.nextgenlab.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.nextgenlab.game.crafting.Item;
import com.nextgenlab.game.crafting.ItemType;
import com.nextgenlab.game.crafting.Resource;
import com.nextgenlab.game.entity.Researcher;

import java.util.EnumMap;
import java.util.Map;

public class InventoryPanel {

    private final Stage stage;
    private final BitmapFont font;
    private final Texture bgTex;
    private final Texture btnUpTex;
    private final Texture btnOverTex;
    private boolean open = false;
    private Researcher researcher;

    public InventoryPanel() {
        font  = new BitmapFont();
        stage = new Stage(new ScreenViewport());

        bgTex      = solidTex(0f,    0f,    0f,    0.82f);
        btnUpTex   = solidTex(0.2f,  0.2f,  0.2f,  1f);
        btnOverTex = solidTex(0.35f, 0.35f, 0.35f, 1f);
    }

    private void rebuild() {
        stage.clear();

        Label.LabelStyle white = new Label.LabelStyle(font, Color.WHITE);
        Label.LabelStyle cyan  = new Label.LabelStyle(font, Color.CYAN);

        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.font      = font;
        btnStyle.fontColor = Color.WHITE;
        btnStyle.up        = new TextureRegionDrawable(new TextureRegion(btnUpTex));
        btnStyle.over      = new TextureRegionDrawable(new TextureRegion(btnOverTex));

        TextureRegionDrawable bgDrawable = new TextureRegionDrawable(new TextureRegion(bgTex));

        Table root = new Table();
        root.setFillParent(true);
        root.setBackground(bgDrawable);
        root.pad(20).top().left();

        root.add(new Label("INVENTORY  [I / ESC to close]", cyan))
            .left().padBottom(12).row();


        EnumMap<Resource, Integer> inv = researcher.getInventory();
        StringBuilder res = new StringBuilder("Resources:\n");
        for (Resource r : Resource.values()) {
            res.append("  ").append(r.name()).append(": ")
               .append(inv.getOrDefault(r, 0)).append('\n');
        }
        root.add(new Label(res.toString(), white)).left().padBottom(10).row();


        Item w = researcher.getEquippedWeapon();
        root.add(new Label("Weapon: " + (w != null ? w.type.displayName : "—"), white))
            .left().padBottom(6).row();


        root.add(new Label("Utilities (klik untuk pilih):", cyan)).left().padBottom(4).row();

        Map<ItemType, Integer> stock = researcher.getUtilityStock();
        ItemType selected = researcher.getSelectedUtilityType();

        if (stock.isEmpty()) {
            root.add(new Label("  (kosong)", white)).left().padBottom(4).row();
        } else {
            for (Map.Entry<ItemType, Integer> entry : stock.entrySet()) {
                ItemType type  = entry.getKey();
                int      count = entry.getValue();
                boolean  isSelected = type == selected;

                TextButton.TextButtonStyle rowStyle = new TextButton.TextButtonStyle();
                rowStyle.font      = font;
                rowStyle.fontColor = isSelected ? Color.CYAN : Color.WHITE;
                rowStyle.up        = new TextureRegionDrawable(new TextureRegion(
                    isSelected ? btnOverTex : btnUpTex));
                rowStyle.over      = new TextureRegionDrawable(new TextureRegion(btnOverTex));

                String label = (isSelected ? "[ACTIVE] " : "") + type.displayName + "  x" + count;
                TextButton btn = new TextButton(label, rowStyle);

                final ItemType t = type;
                btn.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent event, float x, float y) {
                        researcher.setSelectedUtilityType(t);
                        rebuild();
                    }
                });
                root.add(btn).left().padBottom(3).row();
            }
        }

        root.add(new Label("\n[F] gunakan utility terpilih", white)).left().row();
        stage.addActor(root);
    }


    public boolean toggle(Researcher researcher) {
        this.researcher = researcher;
        open = !open;
        if (open) {
            rebuild();
            Gdx.input.setInputProcessor(stage);
        }
        return open;
    }

    public boolean isOpen() { return open; }

    public boolean checkClose() {
        if (!open) return false;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.I)) {
            open = false;
            Gdx.input.setInputProcessor(null);
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
        btnUpTex.dispose();
        btnOverTex.dispose();
    }

    private static Texture solidTex(float r, float g, float b, float a) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(r, g, b, a);
        pm.fill();
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }
}
