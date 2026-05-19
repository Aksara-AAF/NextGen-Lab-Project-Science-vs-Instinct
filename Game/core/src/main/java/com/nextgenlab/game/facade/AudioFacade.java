package com.nextgenlab.game.facade;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import java.util.HashMap;
import java.util.Map;

public class AudioFacade {
    private static AudioFacade instance;
    private final Map<String, Sound> sounds = new HashMap<>();
    private final Map<String, Music>  bgms   = new HashMap<>();
    private float masterVol = 1f;
    private Music  currentBgm     = null;
    private String currentBgmName = null;

    private AudioFacade() {}

    public static AudioFacade getInstance() {
        if (instance == null) instance = new AudioFacade();
        return instance;
    }

    public void loadAll() {
        loadSfx("sfx_footstep_concrete", "audio/sfx_footstep_concrete.ogg");
        loadSfx("sfx_footstep_metal",    "audio/sfx_footstep_metal.ogg");
        loadSfx("sfx_footstep_grass",    "audio/sfx_footstep_grass.ogg");
        loadSfx("sfx_task_complete",     "audio/sfx_task_complete.ogg");
        loadSfx("sfx_alarm",             "audio/sfx_alarm.ogg");
        loadSfx("sfx_monster_growl",     "audio/sfx_monster_growl.ogg");
        loadSfx("sfx_dash",              "audio/sfx_dash.ogg");
        loadSfx("sfx_hit",               "audio/sfx_hit.ogg");
        loadSfx("sfx_levelup",           "audio/sfx_levelup.ogg");
        loadSfx("sfx_button_click",      "audio/sfx_button_click.ogg");
        loadSfx("sfx_button_hover",      "audio/sfx_button_hover.ogg");
        loadBgm("bgm_menu",        "audio/bgm_menu.ogg");
        loadBgm("bgm_lab_ambient", "audio/bgm_lab_ambient.ogg");
        loadBgm("bgm_duel",        "audio/bgm_duel.ogg");
    }

    private void loadSfx(String name, String path) {
        if (Gdx.files.internal(path).exists())
            sounds.put(name, Gdx.audio.newSound(Gdx.files.internal(path)));
    }

    private void loadBgm(String name, String path) {
        if (Gdx.files.internal(path).exists())
            bgms.put(name, Gdx.audio.newMusic(Gdx.files.internal(path)));
    }

    public void playSfx(String name) {
        Sound s = sounds.get(name);
        if (s != null) s.play(masterVol);
    }

    public void playFootstep() {
        String[] variants = {"sfx_footstep_concrete", "sfx_footstep_metal", "sfx_footstep_grass"};
        playSfx(variants[(int)(Math.random() * variants.length)]);
    }

    public void playBgm(String name) {
        if (name.equals(currentBgmName)) return;
        if (currentBgm != null) currentBgm.stop();
        currentBgmName = name;
        currentBgm = bgms.get(name);
        if (currentBgm != null) {
            currentBgm.setVolume(masterVol * 0.65f);
            currentBgm.setLooping(true);
            currentBgm.play();
        }
    }

    public void stopBgm() {
        if (currentBgm != null) currentBgm.stop();
        currentBgm     = null;
        currentBgmName = null;
    }

    public void setMasterVol(float v) {
        masterVol = Math.max(0f, Math.min(1f, v));
        if (currentBgm != null) currentBgm.setVolume(masterVol * 0.65f);
    }

    public void dispose() {
        if (currentBgm != null) currentBgm.stop();
        sounds.values().forEach(Sound::dispose);
        bgms.values().forEach(Music::dispose);
        sounds.clear();
        bgms.clear();
        currentBgm     = null;
        currentBgmName = null;
    }
}
