package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.Camera;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Lightmap.class)
public abstract class MixinLightmap {
    @Unique private static final Vector3fc NOAMMADDONS$WHITE = new Vector3f(1f, 1f, 1f);

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    private LightmapRenderState noammaddons$fullbright(LightmapRenderState renderState) {
        if (!Camera.INSTANCE.enabled || !Camera.getFullBright().getValue()) return renderState;

        renderState.needsUpdate = Camera.flashFullFright;
        Camera.flashFullFright = false;
        renderState.skyFactor = 1f;
        renderState.blockFactor = 1f;
        renderState.nightVisionEffectIntensity = 0f;
        renderState.darknessEffectScale = 0f;
        renderState.bossOverlayWorldDarkening = 0f;
        renderState.brightness = 1f;
        renderState.blockLightTint = NOAMMADDONS$WHITE;
        renderState.skyLightColor = NOAMMADDONS$WHITE;
        renderState.ambientColor = NOAMMADDONS$WHITE;
        renderState.nightVisionColor = NOAMMADDONS$WHITE;

        return renderState;
    }
}