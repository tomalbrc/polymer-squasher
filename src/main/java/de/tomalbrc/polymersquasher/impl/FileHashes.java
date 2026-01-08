package de.tomalbrc.polymersquasher.impl;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import de.tomalbrc.polymersquasher.PolymerSquasher;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class FileHashes {
    final Map<String, Long> hashes = new Object2LongLinkedOpenHashMap<>();
    final static MessageDigest MESSAGE_DIGEST;

    static {
        try {
            MESSAGE_DIGEST = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    Path hashesPath() {
        return FabricLoader.getInstance().getGameDir().resolve(ModConfig.getInstance().hashFilePath);
    }


    public Long add(String path, byte[] data) {
        Long newHash = hash(data);
        Long existing = hashes.put(path, newHash);
        if (existing == null) {
            return newHash;
        } else {
            boolean sameHash = existing.equals(newHash);

            if (!sameHash) {
                if (ModConfig.getInstance().logHashMismatch)
                    PolymerSquasher.LOGGER.info("Different hash: {}", path);

                if (ModConfig.getInstance().ignoreHashPaths != null) {
                    for (String s : ModConfig.getInstance().ignoreHashPaths) {
                        if (path.startsWith(s)) {
                            PolymerSquasher.LOGGER.info("Ignoring hash mismatch for {}", path);
                            return null;
                        }
                    }
                }

                hashes.put(path, newHash);

                return newHash;
            }

            return null;
        }
    }

    public static Long hash(byte[] data) {
        if (ModConfig.getInstance().forceSizeBasedHash)
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

    public void load() {
        Gson gson = new Gson();
        Type type = new TypeToken<@NotNull Map<String, Long>>() {}.getType();
        try (FileReader reader = new FileReader(hashesPath().toFile())) {
            Map<String, Long> map = gson.fromJson(reader, type);
            this.hashes.clear();
            this.hashes.putAll(map);
        } catch (Exception ignored) {}
    }

    public void save() {
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter(hashesPath().toFile())) {
            gson.toJson(hashes, writer);
            PolymerSquasher.LOGGER.info("Hashed {} files", this.hashes.size());
        } catch (IOException e) {
            PolymerSquasher.LOGGER.error("Could not save hashes!", e);
        }
    }

    public void remove(String string) {
        hashes.remove(string);
    }

    public void clear() {
        hashes.clear();
    }
}
