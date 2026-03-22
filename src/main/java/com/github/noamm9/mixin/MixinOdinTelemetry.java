package com.github.noamm9.mixin;

import com.github.noamm9.NoammAddons;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.odtheking.odin.OdinMod$onInitializeClient$5", remap = false)
public class MixinOdinTelemetry {
    @Dynamic
    @Inject(method = "invokeSuspend", at = @At(value = "HEAD"), cancellable = true, require = 0)
    private void stopTelemetry(Object result, CallbackInfoReturnable ci) {
        NoammAddons.logger.info("Blocked Odin Telemetry");
        ci.cancel();
    }
}