package de.tomalbrc.polymersquasher.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.tomalbrc.polymersquasher.PolymerSquasher;
import de.tomalbrc.polymersquasher.impl.ModConfig;
import eu.pb4.polymer.resourcepack.api.PackResource;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import eu.pb4.polymer.resourcepack.impl.generation.DefaultRPBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(value = DefaultRPBuilder.class, remap = false)
public class DefaultRPBuilderMixin {

    @WrapOperation(method = "lambda$buildResourcePack$16", at = @At(value = "INVOKE", target = "Leu/pb4/polymer/resourcepack/api/ResourcePackBuilder$OutputGenerator;generateFile(Ljava/util/List;Leu/pb4/polymer/resourcepack/api/ResourcePackBuilder$ResourceConverter;Ljava/util/function/Consumer;)Z"))
    private boolean po$onWrite(ResourcePackBuilder.OutputGenerator instance, List<Map.Entry<String, PackResource>> fileMap, ResourcePackBuilder.ResourceConverter resourceConverter, Consumer<String> stringConsumer, Operation<Boolean> original) {
        if (ModConfig.getInstance().enabled) {
            PolymerSquasher.runner.reload();

            if (ModConfig.getInstance().cleanup)
                PolymerSquasher.runner.cleanup(fileMap);

            var hadChange = PolymerSquasher.runner.writeResourcePack(fileMap, resourceConverter);
            boolean success = true;
            if (hadChange) {
                success = PolymerSquasher.runner.invokeSquash();
            }

            if (success) {
                success = PolymerSquasher.runner.copyFinalZip();
            }

            PolymerSquasher.runner.saveHashes();

            if (success)
                return true;
        }

        return original.call(instance, fileMap, resourceConverter, stringConsumer);
    }
}
