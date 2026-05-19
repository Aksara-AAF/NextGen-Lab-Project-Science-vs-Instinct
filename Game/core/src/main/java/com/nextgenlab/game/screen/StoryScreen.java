package com.nextgenlab.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.nextgenlab.game.NextGenLabGame;
import com.nextgenlab.game.facade.AudioFacade;

public class StoryScreen extends ScreenAdapter {

    private static final float CHAR_SPEED = 0.04f;

    private static final String[] PAGES_RESEARCHER = {
        "BRIEFING RAHASIA — PROYEK APEX\nDivisi Alpha, Fasilitas 07",
        "Spesimen 10-X adalah organisme rekayasa genetika\nyang seharusnya menjadi terobosan ilmu pengetahuan.\nMalam ini, sesuatu berubah.",
        "Tugas Anda: selesaikan semua protokol penelitian\nsebelum spesimen keluar kendali.\nBerhati-hatilah."
    };

    private static final String[] PAGES_MONSTER = {
        "KEGELAPAN. LALU CAHAYA.\nAnda terbangun di Fasilitas 07.",
        "Anda adalah Spesimen 10-X — hasil eksperimen yang\ntelah mengkhianati para penciptanya.\nNaluri bertahan hidup mengambil alih.",
        "Satu tujuan: cegah para peneliti menyelesaikan\nprotokol mereka. Apapun caranya."
    };

    private final NextGenLabGame game;
    private final String[]       pages;
    private final Runnable       onComplete;

    private Stage     stage;
    private BitmapFont font;
    private BitmapFont promptFont;
    private Label     textLabel;
    private Label     promptLabel;


    private Texture[] illustrations;
    private Texture   overlayTex;

    private int   currentPage = 0;
    private int   charIndex   = 0;
    private float charTimer   = 0f;

    public StoryScreen(NextGenLabGame game, String role, Runnable onComplete) {
        this.game       = game;
        this.onComplete = onComplete;
        this.pages      = "RESEARCHER".equals(role) ? PAGES_RESEARCHER : PAGES_MONSTER;
    }

    @Override
    public void show() {
        AudioFacade.getInstance().playBgm("bgm_menu");

        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(
            Gdx.files.internal("fonts/Kenney Pixel.ttf"));
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = 22;
        font = gen.generateFont(p);
        p.size = 18;
        promptFont = gen.generateFont(p);
        gen.dispose();


        boolean isResearcher = pages == PAGES_RESEARCHER;
        String roleIllusPath  = isResearcher ? "story/story_researcher.png" : "story/story_monster.png";
        String breachPath     = "story/story_containmentBreach.png";
        Texture roleIllus  = loadOrSolid(roleIllusPath,  0.05f, 0.08f, 0.18f);
        Texture breachIllu = loadOrSolid(breachPath,     0.18f, 0.04f, 0.04f);
        illustrations = new Texture[]{ roleIllus, roleIllus, breachIllu };

        overlayTex = solid1x1(0f, 0f, 0f, 1f);

        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        Label.LabelStyle style       = new Label.LabelStyle(font,       Color.WHITE);
        Label.LabelStyle promptStyle = new Label.LabelStyle(promptFont, new Color(0.7f, 0.7f, 0.7f, 1f));

        textLabel   = new Label("", style);
        textLabel.setWrap(true);
        promptLabel = new Label("[ ENTER / klik untuk lanjut ]", promptStyle);
        promptLabel.setVisible(false);


        TextButton.TextButtonStyle skipStyle = new TextButton.TextButtonStyle();
        skipStyle.font      = promptFont;
        skipStyle.fontColor = new Color(0.5f, 0.5f, 0.5f, 1f);
        TextButton skipBtn = new TextButton("SKIP »", skipStyle);
        skipBtn.setPosition(1280 - 100, 18);
        skipBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { complete(); }
        });


        Table rightPanel = new Table();
        rightPanel.setFillParent(false);
        rightPanel.setPosition(480, 0);
        rightPanel.setSize(800, 720);
        rightPanel.top().left().pad(48f);
        rightPanel.add(textLabel).width(700).padBottom(20).row();
        rightPanel.add(promptLabel).left().row();

        stage.addActor(rightPanel);
        stage.addActor(skipBtn);


        stage.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                String full = pages[currentPage];
                if (charIndex < full.length()) {
                    charIndex = full.length();
                } else {
                    advance();
                }
            }
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.04f, 0.04f, 0.10f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);


        game.batch.begin();
        game.batch.setColor(Color.WHITE);
        game.batch.draw(illustrations[currentPage], 0, 0, 480, 720);
        game.batch.setColor(0f, 0f, 0f, 0.22f);
        game.batch.draw(overlayTex, 0, 0, 480, 720);
        game.batch.setColor(Color.WHITE);
        game.batch.end();


        String fullText = pages[currentPage];
        charTimer += delta;
        while (charTimer >= CHAR_SPEED && charIndex < fullText.length()) {
            charIndex++;
            charTimer -= CHAR_SPEED;
        }
        textLabel.setText(fullText.substring(0, charIndex));

        boolean pageComplete = (charIndex >= fullText.length());
        promptLabel.setVisible(pageComplete);


        if (pageComplete && (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE))) {
            advance();
        }

        stage.act(delta);
        stage.draw();
    }

    private void advance() {
        if (currentPage < pages.length - 1) {
            currentPage++;
            charIndex = 0;
            charTimer = 0f;
            textLabel.setText("");
            promptLabel.setVisible(false);
        } else {
            complete();
        }
    }

    private void complete() {
        onComplete.run();
    }

    @Override
    public void resize(int w, int h) {
        if (stage != null) stage.getViewport().update(w, h, true);
    }

    @Override
    public void hide() { Gdx.input.setInputProcessor(null); }

    @Override
    public void dispose() {
        if (stage     != null) stage.dispose();
        if (font      != null) font.dispose();
        if (promptFont != null) promptFont.dispose();
        if (overlayTex != null) overlayTex.dispose();
        if (illustrations != null) {

            if (illustrations[0] != null) illustrations[0].dispose();
            if (illustrations[2] != null) illustrations[2].dispose();
        }
    }

    private static Texture loadOrSolid(String path, float r, float g, float b) {
        if (Gdx.files.internal(path).exists()) return new Texture(Gdx.files.internal(path));
        return solid1x1(r, g, b, 1f);
    }

    private static Texture solid1x1(float r, float g, float b, float a) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(r, g, b, a); pm.fill();
        Texture t = new Texture(pm); pm.dispose();
        return t;
    }
}
