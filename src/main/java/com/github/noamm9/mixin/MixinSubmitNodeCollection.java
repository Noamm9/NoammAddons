package com.github.noamm9.mixin;

import com.github.noamm9.utils.render.NoammRenderLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubmitNodeCollection.class)
public class MixinSubmitNodeCollection {
    @Shadow @Final public SimpleFeatureRenderPhase afterTerrain;
    @Shadow @Final public SimpleFeatureRenderPhase alwaysOnTop;

    @Inject(method = "submitCustomGeometry", at = @At("HEAD"), cancellable = true)
    private void routeLateOverlays(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer, CallbackInfo ci) {
        SimpleFeatureRenderPhase phase;
        if (NoammRenderLayers.phaseLayers.contains(renderType)) phase = alwaysOnTop;
        else if (NoammRenderLayers.afterTerrainLayers.contains(renderType)) phase = afterTerrain;
        else return;

        phase.submit(new CustomFeatureRenderer.Submit(poseStack.last().copy(), renderType, customGeometryRenderer));
        ci.cancel();
    }
}
