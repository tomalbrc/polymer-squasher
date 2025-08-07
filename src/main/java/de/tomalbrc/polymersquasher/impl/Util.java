package de.tomalbrc.polymeroptimizer.impl;

import de.tomalbrc.polymeroptimizer.PolymerOptimizer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class Util {
    public static final Path PACK = FabricLoader.getInstance().getGameDir().resolve("polymer/pack");

    public static boolean runPackSquash(Path outputPath) {
        try {
            PackSquashRunner.run(outputPath, FabricLoader.getInstance().getGameDir().resolve(ModConfig.getInstance().packsquash), FabricLoader.getInstance().getGameDir().resolve(ModConfig.getInstance().packsquashConfig));
        } catch (IOException | InterruptedException e) {
            PolymerOptimizer.LOGGER.warn("PackSquash failed", e);
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static boolean writeToDirectory(Map<String, byte[]> fileMap, List<BiFunction<String, byte[], byte[]>> converters) {
        try {
            for (Map.Entry<String, byte[]> entry : fileMap.entrySet()) {
                String relativePath = entry.getKey();
                byte[] data = entry.getValue();

                if (relativePath.isBlank()) {
                    PolymerOptimizer.LOGGER.info("Found empty file path!");
                    continue;
                }

                Path fullPath = PACK.resolve(relativePath);
                if (relativePath.endsWith("/")) {
                    PolymerOptimizer.LOGGER.info("This should not happen!");
                    Files.createDirectories(fullPath);
                } else {
                    Files.createDirectories(fullPath.getParent());
                    if (data != null) {
                        for (var conv : converters) {
                            data = conv.apply(relativePath, data);
                            if (data == null) break;
                        }
                        if (data != null) {
                            Files.write(fullPath, data);
                        }
                    }
                }
            }
        } catch (IOException e) {
            PolymerOptimizer.LOGGER.warn("Failed to write to directory!", e);
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
