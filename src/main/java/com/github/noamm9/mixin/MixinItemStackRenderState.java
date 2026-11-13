package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.Tweaks;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.PlayerHeadSpecialRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemStackRenderState.class)
public abstract class MixinItemStackRenderState {
    @Shadow ItemDisplayContext displayContext;
    @Shadow private int activeLayerCount;
    @Shadow private ItemStackRenderState.LayerRenderState[] layers;

    @WrapMethod(method = "submit")
    private void applyLegacyHeldSkull(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords, int outlineColor, Operation<Void> original) {
        if (Tweaks.INSTANCE.enabled && Tweaks.getLegacyHeldSkull().getValue() && displayContext.firstPerson()) {
            for (int i = 0; i < activeLayerCount; i++) {
                // PackDisabler can render non-head items with a player head model.
                if (! (((IItemStackLayerRenderState) layers[i]).getSpecialRenderer() instanceof PlayerHeadSpecialRenderer)) continue;

                poseStack.pushPose();
                Tweaks.transformHead(poseStack, displayContext.leftHand());
                original.call(poseStack, collector, lightCoords, overlayCoords, outlineColor);
                poseStack.popPose();
                return;
            }
        }

        original.call(poseStack, collector, lightCoords, overlayCoords, outlineColor);
    }
}