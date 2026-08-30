package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.NameTagTweaks;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(NameTagFeatureRenderer.class)
public class NameTagFeatureRendererMixin {
    @ModifyArg(
        method = "prepareText",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Font;prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;"
        ),
        index = 6
    )
    private static int modifyNametagBackground(int originalColor) {
        return NameTagTweaks.INSTANCE.enabled && NameTagTweaks.getDisableNametagBackground().getValue() ? 0 : originalColor;
    }

    @ModifyArg(
        method = "prepareText",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Font;prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;"
        ),
        index = 4
    )
    private static boolean modifyShadowArgument(boolean original) {
        return (NameTagTweaks.INSTANCE.enabled && NameTagTweaks.getAddNameTagTextShadow().getValue()) || original;
    }
}
