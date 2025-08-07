package de.tomalbrc.polymersquasher.impl;

import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PackSquashRunner {
    public static void run(Path outputZip, Path packsquashExecutable, Path packsquashToml) throws IOException, InterruptedException {
        Path workingDir = FabricLoader.getInstance().getGameDir();

        ProcessBuilder processBuilder = new ProcessBuilder(packsquashExecutable.toAbsolutePath().toString());
        processBuilder.directory(workingDir.toFile());
        processBuilder.inheritIO();
        processBuilder.redirectInput(ProcessBuilder.Redirect.PIPE);
        if (ModConfig.getInstance().log) processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);
        else processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        processBuilder.redirectErrorStream(true);

        try (InputStream settingsStream = Files.newInputStream(packsquashToml)) {
            Process process = processBuilder.start();
            OutputStream stdin = process.getOutputStream();
            InputStream stdout = new BufferedInputStream(process.getInputStream());

            stdin.write(("pack_directory = \"polymer/pack\"\n").getBytes(StandardCharsets.UTF_8));
            stdin.write(("output_file_path = \"" + outputZip.toString() + "\"\n").getBytes(StandardCharsets.UTF_8));
            settingsStream.transferTo(stdin);
            stdin.close();

            int b;
            while ((b = stdout.read()) != -1) {
                System.out.write(b);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("PackSquash exited with code " + exitCode);
            }
        }
    }
}