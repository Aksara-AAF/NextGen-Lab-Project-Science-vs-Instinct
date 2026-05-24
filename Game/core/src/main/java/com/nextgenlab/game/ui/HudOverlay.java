package com.nextgenlab.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.nextgenlab.game.crafting.Item;
import com.nextgenlab.game.crafting.ItemType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HudOverlay {

    private static final float SLOT_SIZE = 48f;
    private static final float SLOT_PAD  = 6f;

    private static final Color[] ITEM_FALLBACK_COLORS = {
        Color.YELLOW, Color.CYAN, Color.GREEN, new Color(1f, 0.5f, 0f, 1f),
        Color.MAGENTA, Color.RED, Color.GRAY, Color.BLUE
    };


    private static final String[] ALL_GENE_CODES = {
        "PREDATOR_CLAWS","ADRENAL_SURGE","THICK_HIDE","FRENZY","ECHOLOCATION",
        "ACIDIC_BLOOD","REGENERATION","TOXIC_AURA","PHASE_SHIFT","BERSERKER"
    };
    private static final String[] ALL_GENE_PATHS = {
        "evolution/predator_claws.png","evolution/adrenal_surge.png",
        "evolution/thick_hide.png",    "evolution/frenzy.png",
        "evolution/echolocation.png",  "evolution/acidic_blood.png",
        "evolution/regeneration.png",  "evolution/toxic_aura.png",
        "evolution/phase_shift.png",   "evolution/berserker.png"
    };

    private final ShapeRenderer shapes;
    private final BitmapFont    font;
    private final SpriteBatch   hudBatch;
    private final OrthographicCamera hudCamera;
    private final StaminaBar    staminaBar;
    private final Texture[]     itemIcons = new Texture[ItemType.values().length];
    private final Map<String, Texture> geneTex = new HashMap<>();

    public HudOverlay() {
        shapes     = new ShapeRenderer();
        font       = new BitmapFont();
        hudBatch   = new SpriteBatch();
        hudCamera  = new OrthographicCamera();
        staminaBar = new StaminaBar();
        hudCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        loadItemIcons();
    }

    public void resize(int width, int height) {
        hudCamera.setToOrtho(false, width, height);
        hudCamera.update();
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

    private void ensureGeneTex() {
        if (!geneTex.isEmpty()) return;
        for (int i = 0; i < ALL_GENE_CODES.length; i++) {
            if (Gdx.files.internal(ALL_GENE_PATHS[i]).exists()) {
                geneTex.put(ALL_GENE_CODES[i], new Texture(ALL_GENE_PATHS[i]));
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
                                  int monHp, int monMaxHp,
                                  boolean isMonsterView,
                                  boolean panelOpen) {
        int W = Gdx.graphics.getWidth();
        int H = Gdx.graphics.getHeight();
        hudCamera.update();

        shapes.setProjectionMatrix(hudCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);


        if (!panelOpen) {
            shapes.setColor(0.15f, 0.15f, 0.15f, 1f);
            shapes.rect(10, H - 46, 200, 18);
            float fill = totalTasks > 0 ? (float) tasksCompleted / totalTasks : 0f;
            shapes.setColor(Color.GREEN);
            shapes.rect(10, H - 46, 200 * fill, 18);
        }


        shapes.setColor(0.15f, 0.15f, 0.15f, 1f);
        shapes.rect(10, 10, 160, 20);
        shapes.setColor(Color.CYAN);
        shapes.rect(10, 10, resMaxHp > 0 ? 160f * resHp / resMaxHp : 0, 20);


        if (!isMonsterView) staminaBar.render(shapes, 10, 34, 160, 10, stamina, maxStamina);


        float barX = W - 170f;
        shapes.setColor(0.15f, 0.15f, 0.15f, 1f);
        shapes.rect(barX, 10, 160, 20);
        shapes.setColor(Color.RED);
        shapes.rect(barX, 10, monMaxHp > 0 ? 160f * monHp / monMaxHp : 0, 20);

        shapes.end();

        hudBatch.setProjectionMatrix(hudCamera.combined);
        hudBatch.begin();


        if (!panelOpen) {
            font.setColor(Color.WHITE);
            font.draw(hudBatch,
                "PERSIAPAN  " + tasksCompleted + "/" + totalTasks + " task selesai",
                10, H - 24);
            font.setColor(Color.LIGHT_GRAY);
            if (!isMonsterView)
                font.draw(hudBatch, "[E] interaksi  [LMB] tembak  [Shift] sprint  [I] inventori",
                    10, H - 62);
            else
                font.draw(hudBatch, "[LMB] serang  [Shift] dash  [E] sabotase",
                    10, H - 62);
        }


        font.setColor(Color.LIGHT_GRAY);
        font.draw(hudBatch, "Peneliti", 10, 80);
        float monsterLabelY = isMonsterView ? 92f : 50f;
        font.draw(hudBatch, "Monster", barX, monsterLabelY);

        font.setColor(Color.WHITE);
        font.draw(hudBatch, resHp + "/" + resMaxHp, 14, 25);
        font.draw(hudBatch, monHp + "/" + monMaxHp, barX + 4, 25);
        if (!isMonsterView) staminaBar.renderLabel(hudBatch, font, 10, 60, stamina, maxStamina);
        hudBatch.end();
    }

    public void renderDuel(int resHp, int resMaxHp,
                           float stamina, float maxStamina,
                           int monHp, int monMaxHp,
                           boolean isResearcher) {
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
        font.draw(hudBatch, "Peneliti  " + resHp + "/" + resMaxHp, 10, 44);
        staminaBar.renderLabel(hudBatch, font, 10, 62, stamina, maxStamina);
        font.draw(hudBatch, "Monster  "  + monHp + "/" + monMaxHp, barX, 44);
        font.setColor(Color.LIGHT_GRAY);
        String hint = isResearcher ? "[LMB] tembak  [Shift] sprint"
                                   : "[RMB] ranged  [LMB] serang  [Shift] dash";
        font.draw(hudBatch, hint, 10, 64);
        font.setColor(Color.WHITE);
        hudBatch.end();
    }


    public void renderXpBar(int xp, int level, int maxLevel, int threshold) {
        int W = Gdx.graphics.getWidth();
        int H = Gdx.graphics.getHeight();
        float barX = W - 210f;
        float barY = H - 46f;
        hudCamera.update();
        shapes.setProjectionMatrix(hudCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.15f, 0.15f, 0.15f, 1f);
        shapes.rect(barX, barY, 200, 18);
        float fill = (level < maxLevel && threshold > 0) ? Math.min(1f, (float) xp / threshold) : 1f;
        shapes.setColor(Color.YELLOW);
        shapes.rect(barX, barY, 200 * fill, 18);
        shapes.end();
        hudBatch.setProjectionMatrix(hudCamera.combined);
        hudBatch.begin();
        font.setColor(Color.YELLOW);
        String label = level >= maxLevel
            ? "EVOLUSI  Lv MAX"
            : "EVOLUSI  Lv " + level + "  " + xp + "/" + threshold;
        font.draw(hudBatch, label, barX, H - 24);
        hudBatch.end();
    }


    public void renderDashCooldown(float cooldownTimer, float maxCooldown, Texture dashIcon) {
        int W = Gdx.graphics.getWidth();
        float barX = W - 170f;
        float barY = 50f;
        hudCamera.update();
        shapes.setProjectionMatrix(hudCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.15f, 0.15f, 0.15f, 1f);
        shapes.rect(barX + 44, barY, 116, 10);
        float ready = maxCooldown > 0 ? 1f - Math.min(1f, cooldownTimer / maxCooldown) : 1f;
        shapes.setColor(cooldownTimer <= 0 ? Color.ORANGE : new Color(0.5f, 0.3f, 0f, 1f));
        shapes.rect(barX + 44, barY, 116 * ready, 10);
        shapes.end();
        hudBatch.setProjectionMatrix(hudCamera.combined);
        hudBatch.begin();
        if (dashIcon != null) {
            hudBatch.draw(dashIcon, barX, barY - 15, 40, 40);
        }
        font.setColor(cooldownTimer <= 0 ? Color.ORANGE : Color.GRAY);
        String label = cooldownTimer <= 0 ? "DASH" : String.format("%.1fs", cooldownTimer);
        font.draw(hudBatch, label, barX + 46, barY + 22);
        hudBatch.end();
    }


    public void renderGenePanel(List<String> activeGenes,
                                float echoloCd, float echoloMax,
                                float phaseCd,  float phaseMax) {
        if (activeGenes == null || activeGenes.isEmpty()) return;
        ensureGeneTex();

        int W = Gdx.graphics.getWidth();
        int H = Gdx.graphics.getHeight();

        final float ICON = 40f;
        final float GAP  = 6f;

        float iconX  = W - 52f;
        float firstY = H - 110f;

        hudCamera.update();


        hudBatch.setProjectionMatrix(hudCamera.combined);
        hudBatch.begin();
        for (int i = 0; i < activeGenes.size(); i++) {
            String  code  = activeGenes.get(i);
            float   iconY = firstY - i * (ICON + GAP);
            Texture tex   = geneTex.get(code);
            if (tex == null) continue;
            float cd  = cooldownFor(code, echoloCd, phaseCd);
            float max = maxCooldownFor(code, echoloMax, phaseMax);
            if (max > 0 && cd > 0) {
                hudBatch.setColor(0.35f, 0.35f, 0.35f, 1f);
            } else {
                hudBatch.setColor(Color.WHITE);
            }
            hudBatch.draw(tex, iconX, iconY, ICON, ICON);
        }
        hudBatch.setColor(Color.WHITE);
        hudBatch.end();


        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(hudCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < activeGenes.size(); i++) {
            String code = activeGenes.get(i);
            float  cd   = cooldownFor(code, echoloCd, phaseCd);
            float  max  = maxCooldownFor(code, echoloMax, phaseMax);
            if (max <= 0 || cd <= 0) continue;
            float fraction = Math.min(1f, cd / max);
            float iconY = firstY - i * (ICON + GAP);
            float cx = iconX + ICON / 2f;
            float cy = iconY + ICON / 2f;
            float r  = ICON / 2f - 1f;
            shapes.setColor(0f, 0f, 0f, 0.70f);
            if (fraction >= 0.999f) {
                shapes.circle(cx, cy, r, 32);
            } else {

                shapes.arc(cx, cy, r, 90f - fraction * 360f, fraction * 360f, 32);
            }
        }
        shapes.end();


        hudBatch.setProjectionMatrix(hudCamera.combined);
        hudBatch.begin();
        for (int i = 0; i < activeGenes.size(); i++) {
            String code = activeGenes.get(i);
            float  cd   = cooldownFor(code, echoloCd, phaseCd);
            float  max  = maxCooldownFor(code, echoloMax, phaseMax);
            if (max <= 0 || cd <= 0) continue;
            float iconY = firstY - i * (ICON + GAP);
            font.setColor(Color.WHITE);
            String text = (int) Math.ceil(cd) + "s";
            font.draw(hudBatch, text, iconX + 10, iconY + ICON / 2f + 6);
        }
        hudBatch.setColor(Color.WHITE);
        hudBatch.end();
    }

    private static float cooldownFor(String code, float echoloCd, float phaseCd) {
        if ("ECHOLOCATION".equals(code)) return echoloCd;
        if ("PHASE_SHIFT".equals(code))  return phaseCd;
        return 0f;
    }

    private static float maxCooldownFor(String code, float echoloMax, float phaseMax) {
        if ("ECHOLOCATION".equals(code)) return echoloMax;
        if ("PHASE_SHIFT".equals(code))  return phaseMax;
        return 0f;
    }

    public void renderLightsOut() {
        int W = Gdx.graphics.getWidth(), H = Gdx.graphics.getHeight();
        hudCamera.update();
        shapes.setProjectionMatrix(hudCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.87f);
        shapes.rect(0, 0, W, H);
        shapes.end();
        hudBatch.setProjectionMatrix(hudCamera.combined);
        hudBatch.begin();
        font.setColor(Color.ORANGE);
        font.draw(hudBatch, "LIGHTS OUT!", W / 2f - 40, H / 2f);
        hudBatch.end();
    }

    public void renderMatchTimer(float secondsLeft) {
        int W = Gdx.graphics.getWidth(), H = Gdx.graphics.getHeight();
        int min = (int)(secondsLeft / 60);
        int sec = (int)(secondsLeft % 60);
        String text = min + ":" + String.format("%02d", sec);
        hudCamera.update();
        hudBatch.setProjectionMatrix(hudCamera.combined);
        hudBatch.begin();
        font.setColor(secondsLeft < 30f ? Color.RED : Color.WHITE);
        com.badlogic.gdx.graphics.g2d.GlyphLayout layout =
            new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, text);
        font.draw(hudBatch, text, (W - layout.width) / 2f, H - 6f);
        font.setColor(Color.WHITE);
        hudBatch.end();
    }

    public void renderAmmo(int ammo, int maxAmmo, int ammoReserve, boolean reloading, float reloadTimer) {
        if (maxAmmo <= 0) return;
        int W = Gdx.graphics.getWidth();
        float slotY  = SLOT_PAD + 80f + SLOT_SIZE + 6f;
        float slot1X = W - 2 * (SLOT_SIZE + SLOT_PAD) - SLOT_PAD;
        hudCamera.update();
        hudBatch.setProjectionMatrix(hudCamera.combined);
        hudBatch.begin();
        if (reloading) {
            font.setColor(Color.YELLOW);
            font.draw(hudBatch, String.format("RELOAD %.1fs  +%d", reloadTimer, ammoReserve), slot1X, slotY);
        } else {
            font.setColor(ammo == 0 ? Color.RED : Color.LIGHT_GRAY);
            font.draw(hudBatch, "AMMO " + ammo + "/" + maxAmmo + "  +" + ammoReserve, slot1X, slotY);
        }
        font.setColor(Color.WHITE);
        hudBatch.end();
    }

    public void renderNotification(String msg) {
        int W = Gdx.graphics.getWidth(), H = Gdx.graphics.getHeight();
        hudCamera.update();
        com.badlogic.gdx.graphics.g2d.GlyphLayout gl =
            new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, msg);
        float textX = (W - gl.width) / 2f;
        float textY = H - 76f;
        float pad = 10f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(hudCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.65f);
        shapes.rect(textX - pad, textY - gl.height - pad + 4f,
                    gl.width + 2 * pad, gl.height + 2 * pad);
        shapes.end();
        hudBatch.setProjectionMatrix(hudCamera.combined);
        hudBatch.begin();
        font.setColor(Color.YELLOW);
        font.draw(hudBatch, msg, textX, textY);
        font.setColor(Color.WHITE);
        hudBatch.end();
    }

    public void dispose() {
        shapes.dispose();
        font.dispose();
        hudBatch.dispose();
        for (Texture t : itemIcons)          { if (t != null) t.dispose(); }
        for (Texture t : geneTex.values())   { if (t != null) t.dispose(); }
    }
}
