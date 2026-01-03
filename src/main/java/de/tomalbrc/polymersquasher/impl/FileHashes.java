package de.tomalbrc.polymersquasher.impl;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import de.tomalbrc.polymersquasher.PolymerSquasher;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Collections;

public class FileHashes {
    final static Path FILE = FabricLoader.getInstance().getGameDir().resolve("polymer/hashes.json");
    private static Map<String, Long> CURRENT_HASHES = new Object2LongLinkedOpenHashMap<>();
    final static MessageDigest MESSAGE_DIGEST;

    static {
        try {
            MESSAGE_DIGEST = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static void clear() {
        CURRENT_HASHES.clear();
    }

    private static boolean isIgnored(String path) {
        if (ModConfig.getInstance().ignoreList != null && !ModConfig.getInstance().ignoreList.isEmpty()) {
            for (String s : ModConfig.getInstance().ignoreList) {
                if (path.startsWith(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void add(String path, byte[] data) {
        if (isIgnored(path)) {
            return;
        }
        CURRENT_HASHES.put(path, hash(data));
    }

    public static Map<String, Long> getHashes() {
        return CURRENT_HASHES;
    }

    public static Long hash(byte[] data) {
        if (ModConfig.getInstance().sizeHash)
            return (long) data.length;

        MESSAGE_DIGEST.update(data);
        return xorFold(MESSAGE_DIGEST.digest());
    }

    static long xorFold(byte[] digest) {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value <<= 8;
            value |= ((digest[i] ^ digest[i + 8]) & 0xFF);
        }
        return value;
    }

    public static Map<String, Long> loadPreviousHashes() {
        if (!Files.exists(FILE)) {
            return Collections.emptyMap();
        }
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Long>>() {}.getType();
        try (FileReader reader = new FileReader(FILE.toFile())) {
            Map<String, Long> loadedHashes = gson.fromJson(reader, type);
            if (loadedHashes != null) {
                return loadedHashes;
            } else {
                return Collections.emptyMap();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Gson gson = new Gson();
            try (FileWriter writer = new FileWriter(FILE.toFile())) {
                gson.toJson(CURRENT_HASHES, writer);
                PolymerSquasher.LOGGER.info("Hashed {} files", CURRENT_HASHES.size());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
