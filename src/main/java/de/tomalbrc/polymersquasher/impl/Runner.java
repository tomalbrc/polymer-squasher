package de.tomalbrc.polymersquasher.impl;

import com.google.gson.*;
import de.tomalbrc.polymersquasher.PolymerSquasher;
import eu.pb4.polymer.resourcepack.api.PackResource;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class Runner {
    FileHashes hashes = new FileHashes();
    Path packDirectory;
    Path minZip;

    public Runner() {
        reload();
    }

    public void reload() {
        ModConfig.load();
        this.hashes.load();

        this.packDirectory = FabricLoader.getInstance().getGameDir().resolve(ModConfig.getInstance().resourcePackDirectory);
        this.minZip = FabricLoader.getInstance().getGameDir().resolve(ModConfig.getInstance().minifiedZipPath);
    }

    public boolean invokeSquash() {
        try {
            PolymerSquasher.LOGGER.info("Running packsquash... This might take a while!");
            this.invoke(FabricLoader.getInstance().getGameDir().resolve(ModConfig.getInstance().packsquashPath), FabricLoader.getInstance().getGameDir().resolve(ModConfig.getInstance().packsquashTomlPath));
        } catch (IOException | InterruptedException e) {
            PolymerSquasher.LOGGER.warn("PackSquash failed", e);
            return false;
        }

        return true;
    }

    private void invoke(Path packsquashExecutable, Path packsquashToml) throws IOException, InterruptedException {
        Path workingDir = FabricLoader.getInstance().getGameDir();

        ProcessBuilder processBuilder = new ProcessBuilder(packsquashExecutable.toAbsolutePath().toString());
        processBuilder.directory(workingDir.toFile());
        processBuilder.inheritIO();
        processBuilder.redirectInput(ProcessBuilder.Redirect.PIPE);
        if (ModConfig.getInstance().logPacksquash) processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);
        else processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        processBuilder.redirectErrorStream(true);

        try (InputStream settingsStream = Files.newInputStream(packsquashToml)) {
            Process process = processBuilder.start();
            OutputStream stdin = process.getOutputStream();
            InputStream stdout = new BufferedInputStream(process.getInputStream());

            stdin.write(("pack_directory = \""+ FilenameUtils.separatorsToUnix(this.packDirectory.toString()) +"\"\n").getBytes(StandardCharsets.UTF_8));
            stdin.write(("output_file_path = \"" + FilenameUtils.separatorsToUnix(this.minZip.toString()) + "\"\n").getBytes(StandardCharsets.UTF_8));
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

    /**
     * Returns true if changes to the rp were made
     */
    public boolean writeResourcePack(List<Map.Entry<String, PackResource>> fileMap, ResourcePackBuilder.ResourceConverter converter) {
        hashes.load();

        boolean dirty = false;
        try {
            for (Map.Entry<String, PackResource> entry : fileMap) {
                String relativePath = entry.getKey();
                if (entry.getValue() == null) {
                    PolymerSquasher.LOGGER.info("Found empty data for path '{}', skipping.", entry.getKey());
                    continue;
                }

                byte[] data = entry.getValue().readAllBytes();

                if (relativePath.isBlank()) {
                    PolymerSquasher.LOGGER.info("Found empty file path, skipping.");
                    continue;
                }

                Path fullPath = this.packDirectory.resolve(relativePath);
                if (relativePath.endsWith("/")) {
                    Files.createDirectories(fullPath);
                } else {
                    if (data != null) {
                        var converted = converter.convert(relativePath, entry.getValue());
                        if (converted == null)
                            continue;

                        data = converted.readAllBytes();
                        if (data == null)
                            continue;


                        if (relativePath.equals("pack.mcmeta")) {
                            JsonObject json = JsonParser.parseReader(new InputStreamReader(new ByteArrayInputStream(data))).getAsJsonObject();
                            JsonElement pack = json.get("pack");
                            JsonObject newPack = pack.getAsJsonObject();
                            if (!newPack.has("pack_format"))
                                newPack.getAsJsonObject().add("pack_format", new JsonPrimitive(64));

                            json.add("pack", newPack);
                            data = new Gson().toJson(json).getBytes(StandardCharsets.UTF_8);

                        }

                        Files.createDirectories(fullPath.getParent());

                        if (fullPath.toFile().exists()) {
                            Long e = this.hashes.add(relativePath, data);
                            if (e == null)
                                continue;
                        }

                        Files.write(fullPath, data);
                        dirty = true;
                    }
                }
            }
        } catch (IOException e) {
            PolymerSquasher.LOGGER.error("Failed to write to directory!", e);
            return false;
        }

        return dirty;
    }

    public boolean copyFinalZip() {
        var outputPath = PolymerResourcePackUtils.getMainPath();
        try {
            Files.copy(this.minZip, outputPath);
        } catch (IOException ignored) {}

        return outputPath.toFile().exists();
    }

    public void saveHashes() {
        this.hashes.save();
    }

    public void cleanup(List<Map.Entry<String, PackResource>> fileMap) {
        try {
            var packPaths = Files.walk(packDirectory);
            var filepaths = fileMap.stream().map(Map.Entry::getKey).collect(Collectors.toSet());

            packPaths.forEach(x -> {
                if (!filepaths.contains(x.relativize(FabricLoader.getInstance().getGameDir()).toString())) {
                    try {
                        Files.delete(x);
                        this.hashes.remove(x.toString());
                    } catch (IOException e) {
                        PolymerSquasher.LOGGER.error("Could not delete old file: {}", x, e);
                    }
                }
            });

            packPaths.close();
        } catch (IOException e) {
            PolymerSquasher.LOGGER.error("Error during clean up", e);
        }
    }
}