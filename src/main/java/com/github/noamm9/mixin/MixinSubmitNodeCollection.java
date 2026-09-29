package com.github.noamm9.mixin;

import com.github.noamm9.utils.render.world.NoammRenderTypes;
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

/**
 * 26.2 has no OIT pipelines, so blended custom geometry lands in the translucent phase and
 * see-through blocks (glass, leaves, ...) draw over it. Route our render types into later phases.
 */
@Mixin(SubmitNodeCollection.class)
public class MixinSubmitNodeCollection {
    @Shadow @Final public SimpleFeatureRenderPhase afterTerrain;
    @Shadow @Final public SimpleFeatureRenderPhase alwaysOnTop;

    @Inject(method = "submitCustomGeometry", at = @At("HEAD"), cancellable = true)
    private void routeNoammRenderTypes(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer, CallbackInfo ci) {
        SimpleFeatureRenderPhase phase;
        if (NoammRenderTypes.alwaysOnTopTypes.contains(renderType)) phase = alwaysOnTop;
        else if (NoammRenderTypes.afterTerrainTypes.contains(renderType)) phase = afterTerrain;
        else return;

        phase.submit(new CustomFeatureRenderer.Submit(poseStack.last().copy(), renderType, customGeometryRenderer));
        ci.cancel();
    }
}
