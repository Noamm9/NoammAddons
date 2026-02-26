package com.github.noamm9.mixin;

import com.github.noamm9.NoammAddons;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.odtheking.odin.OdinMod", remap = false)
public class MixinOdinTelemetry {
    @Dynamic
    @Inject(method = "onInitializeClient()V", at = @At(value = "INVOKE", target = "Lkotlin/text/Regex;<init>(Ljava/lang/String;)V", remap = false), cancellable = true)
    private void stopTelemetry(CallbackInfo ci) {
        ci.cancel();
        NoammAddons.INSTANCE.getLogger().info("Blocked Odin Telemetry");
    }
}