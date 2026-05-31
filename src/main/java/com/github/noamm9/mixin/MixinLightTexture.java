package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.Camera;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.OptionalInt;

@Mixin(Lightmap.class)
public abstract class MixinLightTexture {
    @Shadow @Final private MappableRingBuffer ubo;

    @Shadow @Final private GpuTextureView textureView;

    @Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
    private void renderHook(LightmapRenderState renderState, CallbackInfo ci) {
        if (!renderState.needsUpdate) return;
        ci.cancel();

        ProfilerFiller profiler = Profiler.get();
        profiler.push("lightmap");
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        var fullbright = Camera.INSTANCE.enabled && Camera.getFullBright().getValue();
        var darkness = Camera.INSTANCE.enabled && Camera.getDisableDarkness().getValue() ? 0f : renderState.darknessEffectScale;

        try (GpuBuffer.MappedView view = commandEncoder.mapBuffer(ubo.currentBuffer(), false, true)) {
            Std140Builder.intoBuffer(view.data())
                .putFloat(renderState.skyFactor)
                .putFloat(renderState.blockFactor)
                .putFloat(fullbright ? 1f : renderState.nightVisionEffectIntensity)
                .putFloat(darkness)
                .putFloat(renderState.bossOverlayWorldDarkening)
                .putFloat(fullbright ? 1f : renderState.brightness)
                .putVec3(renderState.blockLightTint)
                .putVec3(renderState.skyLightColor)
                .putVec3(renderState.ambientColor)
                .putVec3(renderState.nightVisionColor);
        }

        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "Update light", textureView, OptionalInt.empty())) {
            renderPass.setPipeline(RenderPipelines.LIGHTMAP);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("LightmapInfo", this.ubo.currentBuffer());
            renderPass.draw(0, 3);
        }

        this.ubo.rotate();
        profiler.pop();
    }
}