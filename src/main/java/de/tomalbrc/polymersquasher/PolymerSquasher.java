package de.tomalbrc.polymeroptimizer;

import com.mojang.logging.LogUtils;
import de.tomalbrc.polymeroptimizer.impl.ModConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class PolymerOptimizer implements ModInitializer {
    public static Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        var tomlName = "packsquash.toml";
        var packsquashConfigPath = FabricLoader.getInstance().getGameDir().resolve(ModConfig.getInstance().packsquashConfig);
        var parent = packsquashConfigPath.getParent();
        if ((((!parent.toFile().exists() && parent.toFile().mkdirs()) || parent.toFile().exists())) && !packsquashConfigPath.toFile().exists()) {
            try (var toml = PolymerOptimizer.class.getResourceAsStream("/" + tomlName)) {
                if (toml != null) Files.copy(toml, packsquashConfigPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) {
                LOGGER.warn("Could not copy {}", tomlName);
            }
        }

        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((minecraftServer, closeableResourceManager) -> {
            ModConfig.load();
        });
    }
}
