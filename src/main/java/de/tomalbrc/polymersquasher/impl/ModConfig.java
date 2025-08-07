package de.tomalbrc.polymersquasher.impl;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class ModConfig {
    static Path CONFIG_FILE_PATH = FabricLoader.getInstance().getConfigDir().resolve("polymer-squasher.json");
    static ModConfig instance;
    static Gson JSON = new GsonBuilder().setPrettyPrinting().create();

    @SerializedName("enabled")
    public boolean enabled = true;

    @SerializedName("log-packsquash")
    public boolean log = false;

    @SerializedName("packsquash-path")
    public String packsquash = "polymer/packsquash";

    @SerializedName("packsquash-toml-path")
    public String packsquashConfig = "polymer/packsquash.toml";

    public static ModConfig getInstance() {
        if (instance == null) {
            if (!load()) // only save if file wasn't just created
                save(); // save since newer versions may contain new options, also removes old options
        }
        return instance;
    }
    public static boolean load() {
        if (!CONFIG_FILE_PATH.toFile().exists()) {
            instance = new ModConfig();
            try (FileOutputStream stream = new FileOutputStream(CONFIG_FILE_PATH.toFile())) {
                stream.write(JSON.toJson(instance).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return true;
        }

        try {
            ModConfig.instance = JSON.fromJson(new FileReader(ModConfig.CONFIG_FILE_PATH.toFile()), ModConfig.class);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        return false;
    }

    private static void save() {
        try (FileOutputStream stream = new FileOutputStream(CONFIG_FILE_PATH.toFile())) {
            stream.write(JSON.toJson(instance).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
