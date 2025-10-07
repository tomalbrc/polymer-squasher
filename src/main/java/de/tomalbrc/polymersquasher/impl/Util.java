package de.tomalbrc.polymersquasher.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
import java.util.List;
import java.util.Map;

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
    public static boolean writeToDirectory(List<Map.Entry<String, PackResource>> fileMap, ResourcePackBuilder.ResourceConverter converter) {
        boolean dirty = false;
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
