package com.nextgenlab.game.teavm;

import com.github.xpenatan.gdx.teavm.backends.shared.config.AssetFileHandle;
import com.github.xpenatan.gdx.teavm.backends.shared.config.compiler.TeaCompiler;
import com.github.xpenatan.gdx.teavm.backends.web.config.backend.WebBackend;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
            .setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE)
            .setMainClass(TeaVMLauncher.class.getName())
            .setObfuscated(!debug)
            .setDebugInformationGenerated(debug)
            .setSourceMapsFileGenerated(debug)
            .setSourceFilePolicy(TeaVMSourceFilePolicy.COPY)
            .addSourceFileProvider(new DirectorySourceFileProvider(new File("../core/src/main/java/")))
            .build(new File("build/dist"));

        injectScripts(new File("build/dist/webapp/index.html"));
    }

    private static void injectScripts(File indexFile) {
        if (!indexFile.exists()) return;
        try {
            Path path = indexFile.toPath();
            String html = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            String marker = "<script type=\"text/javascript\" charset=\"utf-8\" src=\"app.js\"></script>";
            if (html.contains(marker) && !html.contains("scripts/howler.js")) {
                String injection =
                    "<script type=\"text/javascript\" src=\"scripts/howler.js\"></script>\n        " +
                    "<script type=\"text/javascript\" src=\"scripts/gdx.wasm.js\"></script>\n        " +
                    marker;
                html = html.replace(marker, injection);
                Files.write(path, html.getBytes(StandardCharsets.UTF_8));
                System.out.println("[TeaVMBuilder] index.html patched: scripts/howler.js + scripts/gdx.wasm.js injected.");
            }
        } catch (IOException e) {
            System.err.println("[TeaVMBuilder] Warning: could not patch index.html: " + e.getMessage());
        }
    }
}
