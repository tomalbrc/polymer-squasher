package de.tomalbrc.polymersquasher.mixin;

import de.tomalbrc.polymersquasher.impl.FileHashes;
import de.tomalbrc.polymersquasher.impl.ModConfig;
import de.tomalbrc.polymersquasher.impl.Util;
import eu.pb4.polymer.resourcepack.impl.generation.DefaultRPBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;

@Mixin(value = DefaultRPBuilder.class, remap = false)
public class DefaultRPBuilderMixin {

    @Shadow @Final private Path outputPath;

    @Shadow @Final private HashMap<String, byte[]> fileMap;

    @Shadow @Final private List<BiFunction<String, byte[], byte[]>> converters;

    @Inject(method = "writeSingleZip", at = @At("HEAD"), cancellable = true)
    private void po$onWrite(CallbackInfoReturnable<Boolean> cir) {
        if (ModConfig.getInstance().enabled) {
            FileHashes.load();

            var hadChange = Util.writeToDirectory(fileMap, converters);
            if (hadChange) {
                Util.runPackSquash(Util.MIN_FILE);
            }

            try {
                Files.copy(Util.MIN_FILE, outputPath);
            } catch (IOException ignored) {}

            if (outputPath.toFile().exists()) {
                cir.setReturnValue(true);
            }
            else {
                try {
                    Files.deleteIfExists(outputPath);
                } catch (IOException ignored) {}
            }

            FileHashes.save();
        }
    }
}
