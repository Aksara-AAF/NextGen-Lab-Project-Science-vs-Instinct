package com.nextgenlab.game.task;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class WireTask extends LabTask {

    private static final int    TOTAL_WIRES = 3;
    private static final String[] WIRE_NAMES  = {"Kabel Merah", "Kabel Kuning", "Kabel Biru"};
    private static final Color[]  WIRE_COLORS = {Color.RED, Color.YELLOW, Color.CYAN};

    private int wiresConnected = 0;
    private Label statusLabel;

    private Texture panelTex, btnUpTex, btnDownTex;

    @Override
    protected void buildUI() {
        BitmapFont font = new BitmapFont();

        panelTex   = solidTex(320, 210, 0.05f, 0.05f, 0.15f, 0.93f);
        btnUpTex   = solidTex(260, 44,  0.22f, 0.22f, 0.28f, 1f);
        btnDownTex = solidTex(260, 44,  0.10f, 0.10f, 0.13f, 1f);

        TextureRegionDrawable panelBg = wrap(panelTex);
        TextureRegionDrawable up      = wrap(btnUpTex);
        TextureRegionDrawable down    = wrap(btnDownTex);

        Table panel = new Table();
        panel.setBackground(panelBg);
        panel.setFillParent(true);

        panel.add(new Label("SAMBUNGKAN KABEL", new Label.LabelStyle(font, Color.CYAN)))
            .padBottom(6).row();

        statusLabel = new Label("0 / " + TOTAL_WIRES + " tersambung",
            new Label.LabelStyle(font, Color.WHITE));
        panel.add(statusLabel).padBottom(14).row();

        for (int i = 0; i < TOTAL_WIRES; i++) {
            TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
            style.font      = font;
            style.fontColor = WIRE_COLORS[i];
            style.up        = up;
            style.down      = down;
            style.disabled  = down;

            TextButton btn = new TextButton("Sambungkan " + WIRE_NAMES[i], style);
            btn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    btn.setDisabled(true);
                    wiresConnected++;
                    statusLabel.setText(wiresConnected + " / " + TOTAL_WIRES + " tersambung");
                    if (wiresConnected >= TOTAL_WIRES) finishTask();
                }
            });
            panel.add(btn).width(260).height(40).padBottom(8).row();
        }

        stage.addActor(panel);
    }

    @Override
    public void dispose() {
        super.dispose();
        panelTex.dispose();
        btnUpTex.dispose();
        btnDownTex.dispose();
    }

    private Texture solidTex(int w, int h, float r, float g, float b, float a) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(r, g, b, a);
        p.fill();
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    private TextureRegionDrawable wrap(Texture t) {
        return new TextureRegionDrawable(new TextureRegion(t));
    }
}
