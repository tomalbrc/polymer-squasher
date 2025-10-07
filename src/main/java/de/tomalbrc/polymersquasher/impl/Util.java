package de.tomalbrc.polymersquasher.impl;

import de.tomalbrc.polymersquasher.PolymerSquasher;
import eu.pb4.polymer.resourcepack.api.PackResource;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class Util {
    public static final Path PACK = FabricLoader.getInstance().getGameDir().resolve("polymer/pack");
    public static final Path MIN_FILE = FabricLoader.getInstance().getGameDir().resolve("polymer/resource_pack.min.zip");

    public static boolean runPackSquash(Path outputPath) {
        try {
            PolymerSquasher.LOGGER.info("Running packsquash... This might take a while!");
            PackSquashRunner.run(outputPath, FabricLoader.getInstance().getGameDir().resolve(ModConfig.getInstance().packsquash), FabricLoader.getInstance().getGameDir().resolve(ModConfig.getInstance().packsquashConfig));
        } catch (IOException | InterruptedException e) {
            PolymerSquasher.LOGGER.warn("PackSquash failed", e);
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Returns true if changes to the rp were made
     */
    public static boolean writeToDirectory(Map<String, PackResource> fileMap, List<ResourcePackBuilder.ResourceConverter> converters) {
        boolean dirty = false;
        try {
            for (Map.Entry<String, PackResource> entry : fileMap.entrySet()) {
                String relativePath = entry.getKey();
                byte[] data = entry.getValue().readAllBytes();

                if (relativePath.isBlank()) {
                    var s = new String(data);
                    PolymerSquasher.LOGGER.info("Found empty file path! {}", s);
                    continue;
                }

                Path fullPath = PACK.resolve(relativePath);
                if (relativePath.endsWith("/")) {
                    PolymerSquasher.LOGGER.info("This should not happen!");
                    Files.createDirectories(fullPath);
                } else {

                    if (data != null) {
                        for (var conv : converters) {
                            var converted = conv.convert(relativePath, entry.getValue());
                            if (converted == null)
                                break;

                            data = converted.readAllBytes();
                            if (data == null) break;
                        }
                    }

                    if (data != null) {
                        Files.createDirectories(fullPath.getParent());

                        if (fullPath.toFile().exists()) {
                            var e = FileHashes.addExists(relativePath, data);
                            if (e)
                                continue;
                        }

                        Files.write(fullPath, data);
                        dirty = true;
                    }
                }
            }
        } catch (IOException e) {
            PolymerSquasher.LOGGER.warn("Failed to write to directory!", e);
            return false;
        }

        return dirty;
    }
}
