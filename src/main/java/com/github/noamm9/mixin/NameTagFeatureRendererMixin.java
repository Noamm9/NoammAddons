package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.NameTagTweaks;
//? if <26.3 {
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
//? } else {
/*import net.minecraft.client.renderer.SubmitNodeCollection;
*///? }
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

//? if <26.3 {
@Mixin(NameTagFeatureRenderer.class)
//? } else {
/*@Mixin(SubmitNodeCollection.class)
*///? }
public class NameTagFeatureRendererMixin {
    //? if <26.2 {
    @ModifyArg(
        method = "renderTranslucent",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4fc;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V"
        ),
        index = 8
    )
    private int modifyNametagBackground(int originalColor) {
    //? } else if <26.3 {
    /*@ModifyArg(
        method = "prepareText",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Font;prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;"
        ),
        index = 6
    )
    private static int modifyNametagBackground(int originalColor) {*/
    //? } else {
    /*@ModifyArg(method = "nameTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/TextFeatureRenderer$Content$Text;<init>(FFLnet/minecraft/util/FormattedCharSequence;ZIII)V"), index = 5)
    private static int modifyNametagBackground(int originalColor) {
    *///? }
        return NameTagTweaks.INSTANCE.enabled && NameTagTweaks.getDisableNametagBackground().getValue() ? 0 : originalColor;
    }

    //? if <26.2 {
    @ModifyArg(
        method = "renderTranslucent",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4fc;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V"
        ),
        index = 4
    )
    private boolean modifyShadowArgument(boolean original) {
    //? } else if <26.3 {
    /*@ModifyArg(
        method = "prepareText",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Font;prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;"
        ),
        index = 4
    )
    private static boolean modifyShadowArgument(boolean original) {*/
    //? } else {
    /*@ModifyArg(method = "nameTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/TextFeatureRenderer$Content$Text;<init>(FFLnet/minecraft/util/FormattedCharSequence;ZIII)V"), index = 3)
    private static boolean modifyShadowArgument(boolean original) {
    *///? }
        return (NameTagTweaks.INSTANCE.enabled && NameTagTweaks.getAddNameTagTextShadow().getValue()) || original;
    }
}