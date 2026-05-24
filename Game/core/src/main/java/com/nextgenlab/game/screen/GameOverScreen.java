package com.nextgenlab.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.nextgenlab.game.NextGenLabGame;
import com.nextgenlab.game.facade.AudioFacade;

public class GameOverScreen extends ScreenAdapter {

    private static final float CHAR_SPEED = 0.04f;

    private static final String OUTRO_RESEARCHER =
        "Para peneliti berhasil menyelesaikan semua protokol\nsebelum spesimen dapat menghalangi mereka.\nProject Apex akan dilanjutkan... untuk saat ini.";
    private static final String OUTRO_MONSTER =
        "Spesimen 10-X terbukti tak terkalahkan.\nFasilitas Alpha kini dalam kegelapan.\nTidak ada yang tersisa untuk melaporkan insiden ini.";

    private final NextGenLabGame game;
    private final String         winner;

    private Stage      stage;
    private BitmapFont font;
    private BitmapFont titleFont;
    private Texture    btnTex, btnSelTex;
    private Texture    winTex;

    private Label achievementLabel;
    private Label outroLabel;
    private Table buttonsTable;

    private final String outroText;
    private int   charIndex = 0;
    private float charTimer = 0f;
    private boolean typewriterDone = false;

    public GameOverScreen(NextGenLabGame game, String winner) {
        this.game      = game;
        this.winner    = winner;
        this.outroText = "RESEARCHER".equals(winner) ? OUTRO_RESEARCHER : OUTRO_MONSTER;
    }

    @Override
    public void show() {
        AudioFacade.getInstance().stopBgm();

        font      = new BitmapFont(Gdx.files.internal("fonts/VCR_OSD_MONO_1.001_20.fnt"));
        titleFont = new BitmapFont(Gdx.files.internal("fonts/VCR_OSD_MONO_1.001_40.fnt"));

        btnTex    = solid(220, 44, 0.15f, 0.15f, 0.20f, 1f);
        btnSelTex = solid(220, 44, 0.08f, 0.08f, 0.12f, 1f);


        String winPath = "RESEARCHER".equals(winner)
            ? "background/win_researcher.png"
            : "background/win_monster.png";
        winTex = Gdx.files.internal(winPath).exists()
            ? new Texture(Gdx.files.internal(winPath))
            : solid(1, 1,
                "RESEARCHER".equals(winner) ? 0.05f : 0.20f,
                "RESEARCHER".equals(winner) ? 0.12f : 0.03f,
                "RESEARCHER".equals(winner) ? 0.22f : 0.03f, 1f);

        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);


        String resultText = "RESEARCHER".equals(winner) ? "PENELITI MENANG!" : "MONSTER MENANG!";
        Label resultLabel = new Label(resultText, new Label.LabelStyle(titleFont, Color.GOLD));

        outroLabel       = new Label("", new Label.LabelStyle(font, new Color(0.85f, 0.85f, 0.85f, 1f)));
        outroLabel.setWrap(true);
        achievementLabel = new Label("", new Label.LabelStyle(font, Color.GREEN));

        buttonsTable = new Table();
        buttonsTable.getColor().a = 0f;
        boolean hasRoom = game.currentRoomCode != null;
        if (hasRoom) {
            buttonsTable.add(makeBtn("MAIN LAGI", Color.GREEN, this::onRematch))
                .width(220).height(44).padBottom(10).row();
        }
        buttonsTable.add(makeBtn("KELUAR", Color.RED, this::onLeave))
            .width(220).height(44).row();

        Table rightPanel = new Table();
        rightPanel.setPosition(640, 0);
        rightPanel.setSize(640, 720);
        rightPanel.top().left().pad(40f);
        rightPanel.add(resultLabel).padBottom(28).left().row();
        rightPanel.add(outroLabel).width(560).padBottom(24).left().row();
        rightPanel.add(achievementLabel).padBottom(16).left().row();
        rightPanel.add(buttonsTable).padTop(16).center().row();

        stage.addActor(rightPanel);

        if (game.backend.isLoggedIn() && game.currentMatchId != null) {
            game.backend.finishMatch(game.currentMatchId, winner, responseJson ->
                Gdx.app.postRunnable(() -> showAchievements(responseJson)));
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.03f, 0.03f, 0.08f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);


        stage.getViewport().apply(true);
        game.batch.setProjectionMatrix(stage.getCamera().combined);
        game.batch.begin();
        game.batch.setColor(Color.WHITE);
        game.batch.draw(winTex, 0, 0, 640, 720);
        game.batch.end();


        if (!typewriterDone) {
            charTimer += delta;
            while (charTimer >= CHAR_SPEED && charIndex < outroText.length()) {
                charIndex++;
                charTimer -= CHAR_SPEED;
            }
            outroLabel.setText(outroText.substring(0, charIndex));
            if (charIndex >= outroText.length()) {
                typewriterDone = true;
                buttonsTable.addAction(Actions.sequence(
                    Actions.delay(0.8f),
                    Actions.fadeIn(0.5f)
                ));
            }
        }

        stage.act(delta);
        stage.draw();
    }

    private void onRematch() {
        String code = game.currentRoomCode;
        if (code != null && game.backend.isLoggedIn()) {
            game.backend.resetRoom(code,
                r -> Gdx.app.postRunnable(() -> game.setScreen(new RoomScreen(game, code))),
                e -> Gdx.app.postRunnable(() -> game.setScreen(new RoomScreen(game, code))));
        } else {
            game.setScreen(new RoomScreen(game, code));
        }
    }

    private void onLeave() {
        String code = game.currentRoomCode;
        game.currentRoomCode = null;
        game.resetSession();
        if (code != null && game.backend.isLoggedIn()) {
            game.backend.leaveRoom(code, r -> {}, e -> {});
        }
        game.setScreen(new LobbyScreen(game));
    }

    private void showAchievements(String json) {
        String[] names = parseStringArray(json);
        if (names.length == 0) return;
        StringBuilder sb = new StringBuilder("ACHIEVEMENT BARU:");
        for (String n : names) sb.append("\n  ").append(n);
        achievementLabel.setText(sb.toString());
    }

    private static String[] parseStringArray(String json) {
        if (json == null) return new String[0];
        String s = json.trim();
        if (s.equals("[]") || s.isEmpty()) return new String[0];
        if (s.startsWith("[")) s = s.substring(1);
        if (s.endsWith("]"))   s = s.substring(0, s.length() - 1);
        if (s.trim().isEmpty()) return new String[0];
        String[] parts = s.split(",");
        String[] result = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.startsWith("\"")) part = part.substring(1);
            if (part.endsWith("\""))   part = part.substring(0, part.length() - 1);
            result[i] = part;
        }
        return result;
    }

    private TextButton makeBtn(String text, Color color, Runnable action) {
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        s.font = font; s.fontColor = color;
        s.up   = new TextureRegionDrawable(new TextureRegion(btnTex));
        s.down = new TextureRegionDrawable(new TextureRegion(btnSelTex));
        TextButton btn = new TextButton(text, s);
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                AudioFacade.getInstance().playSfx("sfx_button_click");
                action.run();
            }
        });
        btn.addListener(new InputListener() {
            @Override public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1) AudioFacade.getInstance().playSfx("sfx_button_hover");
            }
        });
        return btn;
    }

    @Override
    public void resize(int width, int height) {
        if (stage != null) stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() { Gdx.input.setInputProcessor(null); }

    @Override
    public void dispose() {
        if (stage     != null) stage.dispose();
        if (font      != null) font.dispose();
        if (titleFont != null) titleFont.dispose();
        if (btnTex    != null) btnTex.dispose();
        if (btnSelTex != null) btnSelTex.dispose();
        if (winTex    != null) winTex.dispose();
    }

    private static Texture solid(int w, int h, float r, float g, float b, float a) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(r, g, b, a); p.fill();
        Texture t = new Texture(p); p.dispose();
        return t;
    }
}
