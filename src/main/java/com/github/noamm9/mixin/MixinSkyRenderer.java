package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.TimeChanger;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SkyRenderer.class)
public class MixinSkyRenderer {
    @WrapMethod(method = "extractRenderState")
    private void onExtractRenderState(ClientLevel level, float partialTicks, Camera camera, SkyRenderState state, Operation<Void> original) {
        TimeChanger.extractingSky = TimeChanger.INSTANCE.enabled;
        original.call(level, partialTicks, camera, state);
        TimeChanger.extractingSky = false;
    }
}
