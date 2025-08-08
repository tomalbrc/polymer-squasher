package de.tomalbrc.polymersquasher.impl;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import de.tomalbrc.polymersquasher.PolymerSquasher;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class FileHashes {
    final static Path FILE = FabricLoader.getInstance().getGameDir().resolve("polymer/hashes.json");
    static Map<String, Long> HASHES = new Object2LongLinkedOpenHashMap<>();
    final static MessageDigest MESSAGE_DIGEST;

    static {
        try {
            MESSAGE_DIGEST = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean addExists(String path, byte[] data) {
        Long hashed = hash(data);
        Long existing;
        if ((existing = HASHES.put(path, hashed)) == null) {
            HASHES.put(path, hashed);
            return false;
        } else {
            boolean sameHash = existing.equals(hash(data));
            if (!sameHash) {
                HASHES.put(path, hashed);
                PolymerSquasher.LOGGER.info("Different hash: {}", path);

                if (ModConfig.getInstance().ignoreList != null && !ModConfig.getInstance().ignoreList.isEmpty()) {
                    for (String s : ModConfig.getInstance().ignoreList) {
                        if (path.startsWith(s)) {
                            PolymerSquasher.LOGGER.info("Ignoring different hash for {}", path);
                            return true;
                        }
                    }
                }
            }
            return sameHash;
        }
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

    public static void load() {
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Long>>() {}.getType();
        try (FileReader reader = new FileReader(FILE.toFile())) {
            HASHES = gson.fromJson(reader, type);
        } catch (FileNotFoundException ignored) {

        }  catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter(FILE.toFile())) {
            gson.toJson(HASHES, writer);
            PolymerSquasher.LOGGER.info("Hashed {} files", HASHES.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
