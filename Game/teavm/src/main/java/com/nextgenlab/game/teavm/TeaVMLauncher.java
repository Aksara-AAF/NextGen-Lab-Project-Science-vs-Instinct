package com.nextgenlab.game.teavm;

import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import com.nextgenlab.game.NextGenLabGame;


public class TeaVMLauncher {

    public static void main(String[] args) {
        WebApplicationConfiguration config = new WebApplicationConfiguration("canvas");
        config.width  = 1280;
        config.height = 720;
        new WebApplication(new NextGenLabGame(), config);
    }
}
