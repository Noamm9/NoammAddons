package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.NameTagTweaks;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(NameTagFeatureRenderer.class)
public class NameTagFeatureRendererMixin {
    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V"
        ),
        index = 8
    )
    private int modifyNametagBackground(int originalColor) {
        return NameTagTweaks.INSTANCE.enabled && NameTagTweaks.getDisableNametagBackground().getValue() ? 0 : originalColor;
    }

    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V"
        ),
        index = 4
    )
    private boolean modifyShadowArgument(boolean original) {
        return (NameTagTweaks.INSTANCE.enabled && NameTagTweaks.getAddNameTagTextShadow().getValue()) || original;
    }
}