package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.BlockOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public abstract class MixinLunarBlockOutline {
    @Dynamic("Added by Lunar Client")
    @Inject(method = "lunar$consumeBlockOutlineRenderState", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private void cancelBlockOutline(CallbackInfoReturnable<BlockOutlineRenderState> cir) {
        if (BlockOverlay.INSTANCE.enabled && !Minecraft.getInstance().gui.hud.isHidden()) cir.setReturnValue(null);
    }
}
