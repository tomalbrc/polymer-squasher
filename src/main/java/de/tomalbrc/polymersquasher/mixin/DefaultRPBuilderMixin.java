package de.tomalbrc.polymersquasher.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.tomalbrc.polymersquasher.impl.FileHashes;
import de.tomalbrc.polymersquasher.impl.ModConfig;
import de.tomalbrc.polymersquasher.impl.Util;
import eu.pb4.polymer.resourcepack.api.PackResource;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import eu.pb4.polymer.resourcepack.impl.generation.DefaultRPBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(value = DefaultRPBuilder.class, remap = false)
public class DefaultRPBuilderMixin {

    // oh god... anyway
    @WrapOperation(method = "lambda$buildResourcePack$16", at = @At(value = "INVOKE", target = "Leu/pb4/polymer/resourcepack/api/ResourcePackBuilder$OutputGenerator;generateFile(Ljava/util/List;Leu/pb4/polymer/resourcepack/api/ResourcePackBuilder$ResourceConverter;Ljava/util/function/Consumer;)Z"))
    private boolean po$onWrite(ResourcePackBuilder.OutputGenerator instance, List<Map.Entry<String, PackResource>> fileMap, ResourcePackBuilder.ResourceConverter resourceConverter, Consumer<String> stringConsumer, Operation<Boolean> original) {
        if (ModConfig.getInstance().enabled) {
            boolean squashed = false;
            try {
                Map<String, Long> oldHashes = FileHashes.loadPreviousHashes();
                Util.writeToDirectory(fileMap, resourceConverter);
                Map<String, Long> newHashes = FileHashes.getHashes();

                var hadChange = !newHashes.equals(oldHashes);

                if (hadChange) {
                    if (Util.runPackSquash(Util.MIN_FILE)) {
                        var outputPath = PolymerResourcePackUtils.getMainPath();
                        try {
                            Files.copy(Util.MIN_FILE, outputPath);
                            squashed = outputPath.toFile().exists();
                        } catch (IOException ignored) {}
                    }
                } else {
                    if (Files.exists(Util.MIN_FILE)) {
                        var outputPath = PolymerResourcePackUtils.getMainPath();
                        try {
                            Files.copy(Util.MIN_FILE, outputPath);
                            squashed = outputPath.toFile().exists();
                        } catch (IOException ignored) {}
                    }
                }
            } finally {
                FileHashes.save();
                Util.deleteDirectoryRecursively(Util.PACK);
            }

            if (squashed) {
                return true;
            }
        }
        return original.call(instance, fileMap, resourceConverter, stringConsumer);
    }
}
