package com.github.noamm9.mixin;

import com.github.noamm9.utils.render.DepthTestGlow;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void tagDepthTestedGlow(Entity entity, EntityRenderState state, float partialTicks, CallbackInfo ci) {
        if (state.outlineColor == 0) return;
        if (! DepthTestGlow.shouldDepthTest(entity)) return;
        state.outlineColor = DepthTestGlow.tag(state.outlineColor);
    }
}