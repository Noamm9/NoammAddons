package com.github.noamm9.mixin;

//? if >=26.3 {

import com.github.noamm9.utils.render.world.RenderBatcher;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    // 26.3 skips the always on top phase unless vanilla has its own gizmos queued that frame
    @Inject(method = "frameHasAlwaysOnTopGizmos", at = @At("HEAD"), cancellable = true)
    private void forceAlwaysOnTopPass(CallbackInfoReturnable<Boolean> cir) {
        if (RenderBatcher.hasAlwaysOnTop) cir.setReturnValue(true);
    }
}
//?}