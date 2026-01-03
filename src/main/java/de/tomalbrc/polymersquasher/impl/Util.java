package de.tomalbrc.polymersquasher.impl;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import de.tomalbrc.polymersquasher.PolymerSquasher;
import eu.pb4.polymer.resourcepack.api.PackResource;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Util {
    public static final Path PACK = FabricLoader.getInstance().getGameDir().resolve("polymer/pack");
    public static final Path MIN_FILE = FabricLoader.getInstance().getGameDir().resolve("polymer/resource_pack.min.zip");

    public static void deleteDirectoryRecursively(Path path) {
        if (Files.exists(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                PolymerSquasher.LOGGER.warn("Failed to delete path: " + p, e);
                            }
                        });
            } catch (IOException e) {
                PolymerSquasher.LOGGER.warn("Failed to walk directory: " + path, e);
            }
        }
    }

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

    public static void writeToDirectory(List<Map.Entry<String, PackResource>> fileMap, ResourcePackBuilder.ResourceConverter converter) {
        FileHashes.clear();
        try {
            for (Map.Entry<String, PackResource> entry : fileMap) {
                String relativePath = entry.getKey();
                if (entry.getValue() == null) {
                    PolymerSquasher.LOGGER.info("Found empty data for path! {}", entry.getKey());
                    continue;
                }

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
                        var converted = converter.convert(relativePath, entry.getValue());
                        if (converted == null)
                            break;

                        data = converted.readAllBytes();
                        if (data == null) break;
                    }

                    if (relativePath.equals("pack.mcmeta")) {
                        var json = JsonParser.parseReader(new InputStreamReader(new ByteArrayInputStream(data))).getAsJsonObject();
                        var pack = json.get("pack");
                        var newPack = pack.getAsJsonObject();
                        if (!newPack.has("pack_format")) newPack.getAsJsonObject().add("pack_format", new JsonPrimitive(64));
                        json.add("pack", newPack);
                        data = new Gson().toJson(json).getBytes(StandardCharsets.UTF_8);
                    }

                    if (data != null) {
                        FileHashes.add(relativePath, data);
                        Files.createDirectories(fullPath.getParent());
                        Files.write(fullPath, data);
                    }
                }
            }
        } catch (IOException e) {
            PolymerSquasher.LOGGER.warn("Failed to write to directory!", e);
        }
    }
}

