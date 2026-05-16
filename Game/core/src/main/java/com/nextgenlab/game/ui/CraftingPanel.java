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
import com.nextgenlab.game.crafting.Recipe;
import com.nextgenlab.game.crafting.RecipeLibrary;
import com.nextgenlab.game.crafting.Resource;
import com.nextgenlab.game.entity.Researcher;

import java.util.Map;

public class CraftingPanel {

    private final Stage stage;
    private final BitmapFont font;
    private final Texture bgTex;
    private final Texture btnUpTex;
    private final Texture btnOverTex;
    private boolean open = false;

    private Researcher researcher;

    public CraftingPanel() {
        font  = new BitmapFont();
        stage = new Stage(new ScreenViewport());

        bgTex    = solidTex(0f,    0f,    0f,    0.88f);
        btnUpTex = solidTex(0.25f, 0.25f, 0.25f, 1f);
        btnOverTex = solidTex(0.45f, 0.45f, 0.45f, 1f);
    }

    public void open(Researcher researcher) {
        this.researcher = researcher;
        this.open = true;
        rebuild();
        Gdx.input.setInputProcessor(stage);
    }

    private void rebuild() {
        stage.clear();

        Label.LabelStyle white  = new Label.LabelStyle(font, Color.WHITE);
        Label.LabelStyle cyan   = new Label.LabelStyle(font, Color.CYAN);
        Label.LabelStyle green  = new Label.LabelStyle(font, Color.GREEN);
        Label.LabelStyle red    = new Label.LabelStyle(font, Color.RED);

        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.font      = font;
        btnStyle.fontColor = Color.WHITE;
        btnStyle.up        = new TextureRegionDrawable(new TextureRegion(btnUpTex));
        btnStyle.over      = new TextureRegionDrawable(new TextureRegion(btnOverTex));

        TextureRegionDrawable bgDrawable = new TextureRegionDrawable(new TextureRegion(bgTex));

        Table root = new Table();
        root.setFillParent(true);
        root.setBackground(bgDrawable);
        root.pad(20).top();

        Label title = new Label("CRAFTING  [ESC / E to close]", cyan);
        root.add(title).colspan(3).left().padBottom(14).row();

        for (Recipe recipe : RecipeLibrary.getAll()) {
            boolean canCraft = recipe.canCraft(researcher.getInventory());

            Label nameLabel = new Label(recipe.output.displayName, white);
            Label ingredientsLabel = new Label(ingredientsText(recipe), canCraft ? green : red);

            TextButton craftBtn = new TextButton("Craft", btnStyle);
            craftBtn.setDisabled(!canCraft);
            craftBtn.getLabel().setColor(canCraft ? Color.WHITE : Color.DARK_GRAY);

            final Recipe r = recipe;
            craftBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (!r.canCraft(researcher.getInventory())) return;
                    researcher.consumeIngredients(r.ingredients);
                    researcher.equipItem(new Item(r.output));
                    open = false;
                    Gdx.input.setInputProcessor(null);
                }
            });

            root.add(nameLabel).left().padRight(12);
            root.add(ingredientsLabel).left().expandX().padRight(12);
            root.add(craftBtn).right().padBottom(8).row();
        }

        stage.addActor(root);
    }

    private String ingredientsText(Recipe recipe) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Resource, Integer> e : recipe.ingredients.entrySet()) {
            int have = researcher.getInventory().getOrDefault(e.getKey(), 0);
            sb.append(e.getKey().name()).append(' ')
              .append(have).append('/').append(e.getValue()).append("  ");
        }
        return sb.toString().trim();
    }

    public boolean isOpen() { return open; }

    public boolean checkClose() {
        if (!open) return false;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.E)) {
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
