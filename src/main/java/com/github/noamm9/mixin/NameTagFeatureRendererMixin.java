package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.NameTagTweaks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

//~ if <26.3 'SubmitNodeCollection' -> 'feature.NameTagFeatureRenderer'
import net.minecraft.client.renderer.SubmitNodeCollection;

//~ if <26.3 'SubmitNodeCollection' -> 'NameTagFeatureRenderer'
@Mixin(SubmitNodeCollection.class)
public class NameTagFeatureRendererMixin {
    //~ if <26.3 '"nameTag"' -> '"prepareText"'
    //~ if <26.3 'Lnet/minecraft/client/renderer/feature/TextFeatureRenderer$Content$Text;<init>(FFLnet/minecraft/util/FormattedCharSequence;ZIII)V' -> 'Lnet/minecraft/client/gui/Font;prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;'
    //~ if <26.3 'index = 5' -> 'index = 6'
    @ModifyArg(method = "nameTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/TextFeatureRenderer$Content$Text;<init>(FFLnet/minecraft/util/FormattedCharSequence;ZIII)V"), index = 5)
    private static int modifyNametagBackground(int originalColor) {
        return NameTagTweaks.INSTANCE.enabled && NameTagTweaks.getDisableNametagBackground().getValue() ? 0 : originalColor;
    }

    //~ if <26.3 '"nameTag"' -> '"prepareText"'
    //~ if <26.3 'Lnet/minecraft/client/renderer/feature/TextFeatureRenderer$Content$Text;<init>(FFLnet/minecraft/util/FormattedCharSequence;ZIII)V' -> 'Lnet/minecraft/client/gui/Font;prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;'
    //~ if <26.3 'index = 3' -> 'index = 4'
    @ModifyArg(method = "nameTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/TextFeatureRenderer$Content$Text;<init>(FFLnet/minecraft/util/FormattedCharSequence;ZIII)V"), index = 3)
    private static boolean modifyShadowArgument(boolean original) {
        return (NameTagTweaks.INSTANCE.enabled && NameTagTweaks.getAddNameTagTextShadow().getValue()) || original;
    }
}