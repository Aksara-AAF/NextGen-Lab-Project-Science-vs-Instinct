package com.nextgenlab.game.teavm;

import com.github.xpenatan.gdx.teavm.backends.shared.config.AssetFileHandle;
import com.github.xpenatan.gdx.teavm.backends.shared.config.compiler.TeaCompiler;
import com.github.xpenatan.gdx.teavm.backends.web.config.backend.WebBackend;
import java.io.File;
import org.teavm.tooling.TeaVMSourceFilePolicy;
import org.teavm.tooling.sources.DirectorySourceFileProvider;
import org.teavm.vm.TeaVMOptimizationLevel;


public class TeaVMBuilder {

    public static void main(String[] args) {
        boolean debug = false;
        boolean startJetty = false;
        for (String arg : args) {
            if ("debug".equals(arg)) debug = true;
            else if ("run".equals(arg))  startJetty = true;
        }

        new TeaCompiler(
            new WebBackend()
                .setHtmlWidth(1280)
                .setHtmlHeight(720)
                .setHtmlTitle("NextGenLab: Science vs Instinct")
                .setStartJettyAfterBuild(startJetty)
                .setJettyPort(8080)
        )
            .addAssets(new AssetFileHandle("../assets"))
            .setOptimizationLevel(debug ? TeaVMOptimizationLevel.SIMPLE : TeaVMOptimizationLevel.ADVANCED)
            .setMainClass(TeaVMLauncher.class.getName())
            .setObfuscated(!debug)
            .setDebugInformationGenerated(debug)
            .setSourceMapsFileGenerated(debug)
            .setSourceFilePolicy(TeaVMSourceFilePolicy.COPY)
            .addSourceFileProvider(new DirectorySourceFileProvider(new File("../core/src/main/java/")))
            .build(new File("build/dist"));
    }
}
