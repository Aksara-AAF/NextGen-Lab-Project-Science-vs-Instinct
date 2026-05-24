package com.nextgenlab.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.nextgenlab.game.task.TaskUiTheme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LevelUpScreen {

    private static final String[] ALL_GENES = {
        "PREDATOR_CLAWS", "ADRENAL_SURGE", "THICK_HIDE", "FRENZY", "ECHOLOCATION",
        "ACIDIC_BLOOD", "REGENERATION", "TOXIC_AURA", "PHASE_SHIFT", "BERSERKER"
    };

    private static final String[] GENE_NAMES = {
        "Predator Claws", "Adrenal Surge", "Thick Hide", "Frenzy", "Echolocation",
        "Acidic Blood", "Regeneration", "Toxic Aura", "Phase Shift", "Berserker"
    };

    private static final String[] GENE_DESCS = {
        "+1 Damage on melee",
        "+20% Movement speed",
        "+1 Max HP",
        "Dash cooldown -1s",
        "Highlight Researcher 3s\n[Q] 30s cooldown",
        "Counter-projectile on hit",
        "+0.5 HP/s out of combat",
        "Slow nearby Researcher",
        "Walk through walls 0.4s\n[G] 20s cooldown",
        "+30% damage at HP < 2"
    };

    private static final String[] GENE_IMAGE_FILES = {
        "evolution/predator_claws.png", "evolution/adrenal_surge.png",
        "evolution/thick_hide.png",     "evolution/frenzy.png",
        "evolution/echolocation.png",   "evolution/acidic_blood.png",
        "evolution/regeneration.png",   "evolution/toxic_aura.png",
        "evolution/phase_shift.png",    "evolution/berserker.png"
    };

    private Stage     stage;
    private Texture[] loadedTextures;
    private boolean   chosen     = false;
    private String    chosenGene = null;

    public void show(List<String> alreadyPicked) {
        stage = new Stage(new FitViewport(1280, 720));

        List<Integer> available = new ArrayList<>();
        for (int i = 0; i < ALL_GENES.length; i++) {
            if (!alreadyPicked.contains(ALL_GENES[i])) available.add(i);
        }
        Collections.shuffle(available);
        List<Integer> options = available.subList(0, Math.min(3, available.size()));

        loadedTextures = new Texture[options.size()];
        for (int i = 0; i < options.size(); i++) {
            int idx = options.get(i);
            if (Gdx.files.internal(GENE_IMAGE_FILES[idx]).exists()) {
                loadedTextures[i] = new Texture(GENE_IMAGE_FILES[idx]);
            }
        }

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Table outer = new Table();
        outer.setBackground(TaskUiTheme.panelBg());
        outer.pad(16);

        Label title = new Label("EVOLUTION — Choose a Gene", TaskUiTheme.titleStyle());
        outer.add(title).colspan(options.size()).padBottom(20).row();

        for (int ci = 0; ci < options.size(); ci++) {
            final int idx   = options.get(ci);
            final String code = ALL_GENES[idx];

            Table card = new Table();
            card.setBackground(TaskUiTheme.panelBg());
            card.pad(10);
            card.defaults().center();


            if (loadedTextures[ci] != null) {
                Image img = new Image(new TextureRegionDrawable(new TextureRegion(loadedTextures[ci])));
                card.add(img).size(64, 64).padBottom(8).row();
            }


            Label nameLbl = new Label(GENE_NAMES[idx], TaskUiTheme.colorStyle(TaskUiTheme.GOLD));
            card.add(nameLbl).padBottom(6).row();


            Label descLbl = new Label(GENE_DESCS[idx], TaskUiTheme.bodyStyle());
            descLbl.setWrap(false);
            card.add(descLbl).padBottom(12).row();


            Label selectLbl = new Label("[PILIH]", TaskUiTheme.colorStyle(TaskUiTheme.CYAN));
            card.add(selectLbl);

            card.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (!chosen) { chosen = true; chosenGene = code; }
                }
            });

            outer.add(card).width(270).fillY().pad(8);
        }

        root.add(outer);
        stage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        Gdx.input.setInputProcessor(stage);
    }

    public boolean isChosen()      { return chosen; }
    public String  getChosenGene() { return chosenGene; }

    public void resize(int width, int height) {
        if (stage != null) stage.getViewport().update(width, height, true);
    }

    public void render(float delta) {
        stage.getViewport().apply(true);
        stage.act(delta);
        stage.draw();
    }

    public void dispose() {
        if (stage != null) { stage.dispose(); stage = null; }
        if (loadedTextures != null) {
            for (Texture t : loadedTextures) { if (t != null) t.dispose(); }
            loadedTextures = null;
        }
    }
}
