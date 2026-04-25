package com.nextgenlab.game.task;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class WireTask extends LabTask {
    private int wiresConnected = 0;
    private final int totalWires = 3;

    @Override
    protected void buildUI() {
        // Skin skin = new Skin(Gdx.files.internal("uiskin.json"));

        Table table = new Table();
        table.setFillParent(true);

        // table.add(new Label("Sambungkan Kabel (Klik 3 kali)", skin)).colspan(2).row();

//        TextButton connectBtn = new TextButton("Sambungkan Kabel Merah", skin);
//        connectBtn.addListener(new ClickListener() {
//            @Override
//            public void clicked(InputEvent event, float x, float y) {
//                wiresConnected++;
//                connectBtn.setDisabled(true);
//
//                if (wiresConnected >= totalWires) {
//                    finishTask();
//                }
//            }
//        });
//        table.add(connectBtn);

        stage.addActor(table);
    }
}
