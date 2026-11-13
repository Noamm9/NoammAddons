package com.github.noamm9.mixin;

import com.github.noamm9.interfaces.ITranslucentEntity;
import com.github.noamm9.utils.render.TranslucentEntity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubmitNodeCollection.class)
public class MixinSubmitNodeCollection {
    @Inject(method = "submitModel", at = @At("HEAD"), cancellable = true)
    private void hideTranslucentGlint(CallbackInfo ci, @Local(argsOnly = true) RenderType renderType) {
        if (TranslucentEntity.isActive() && TranslucentEntity.isGlint(renderType)) ci.cancel();
    }

    @ModifyVariable(method = "submitModel", at = @At("HEAD"), argsOnly = true)
    private RenderType translucentModelRenderType(RenderType renderType) {
        if (! TranslucentEntity.isActive()) return renderType;
        return TranslucentEntity.translucent(renderType);
    }

    @ModifyVariable(method = "submitModel", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int translucentModelColor(int tintedColor) {
        if (! TranslucentEntity.isActive()) return tintedColor;
        return ARGB.multiplyAlpha(tintedColor, TranslucentEntity.opacity);
    }

    @ModifyExpressionValue(method = "submitItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;copy()Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"))
    private PoseStack.Pose tagTranslucentItem(PoseStack.Pose pose) {
        if (TranslucentEntity.isActive()) ((ITranslucentEntity) (Object) pose).noammaddons$setOpacity(TranslucentEntity.opacity);
        return pose;
    }
}
