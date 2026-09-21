package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.NameTagTweaks;
import net.minecraft.client.renderer.SubmitNodeCollection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SubmitNodeCollection.class)
public class NameTagFeatureRendererMixin {
    @ModifyArg(method = "nameTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/TextFeatureRenderer$Content$Text;<init>(FFLnet/minecraft/util/FormattedCharSequence;ZIII)V"), index = 5)
    private static int modifyNametagBackground(int originalColor) {
        return NameTagTweaks.INSTANCE.enabled && NameTagTweaks.getDisableNametagBackground().getValue() ? 0 : originalColor;
    }

    @ModifyArg(method = "nameTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/TextFeatureRenderer$Content$Text;<init>(FFLnet/minecraft/util/FormattedCharSequence;ZIII)V"), index = 3)
    private static boolean modifyShadowArgument(boolean original) {
        return (NameTagTweaks.INSTANCE.enabled && NameTagTweaks.getAddNameTagTextShadow().getValue()) || original;
    }
}