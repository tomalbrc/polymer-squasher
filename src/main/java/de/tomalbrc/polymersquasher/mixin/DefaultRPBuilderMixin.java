package de.tomalbrc.polymersquasher.mixin;

import de.tomalbrc.polymersquasher.impl.FileHashes;
import de.tomalbrc.polymersquasher.impl.ModConfig;
import de.tomalbrc.polymersquasher.impl.Util;
import eu.pb4.polymer.resourcepack.impl.generation.DefaultRPBuilder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;

@Mixin(value = DefaultRPBuilder.class, remap = false)
public class DefaultRPBuilderMixin {
    @Shadow @Final private Path outputPath;

    @Shadow @Final private HashMap<String, byte[]> fileMap;

    @Shadow @Final private List<BiFunction<String, byte[], @Nullable byte[]>> converters;

    @Inject(method = "writeSingleZip", at = @At("HEAD"), cancellable = true)
    private void po$onWrite(CallbackInfoReturnable<Boolean> cir) {
        if (ModConfig.getInstance().enabled) {
            FileHashes.load();

            if ((Util.writeToDirectory(fileMap, converters) || !outputPath.toFile().exists()) && Util.runPackSquash(outputPath)) {
                if (Util.writeToDirectory(fileMap, converters)) {
                    Util.runPackSquash(outputPath);
                }
            }

            if (outputPath.toFile().exists())
                cir.setReturnValue(true);

            FileHashes.save();
        }
    }
}
