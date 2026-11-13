package com.github.noamm9.mixin;

import com.github.noamm9.interfaces.ITranslucentEntity;
import com.github.noamm9.utils.render.DepthTestGlow;
import com.github.noamm9.utils.render.TranslucentEntity;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemFeatureRenderer.class)
public class MixinItemFeatureRenderer {
    @WrapMethod(method = "prepareOutlineSubmit")
    private void beginDepthTestedGlow(ItemFeatureRenderer.Submit submit, Operation<Void> original) {
        DepthTestGlow.active = DepthTestGlow.isTagged(submit.outlineColor());
        original.call(submit);
        DepthTestGlow.active = false;
    }

    @ModifyVariable(method = "prepareMainSubmit", at = @At("STORE"))
    private RenderType translucentItemRenderType(RenderType renderType, @Local(argsOnly = true) ItemFeatureRenderer.Submit submit) {
        if (noammaddons$getOpacity(submit) >= 1f) return renderType;
        return TranslucentEntity.translucent(renderType);
    }

    @ModifyArg(method = "prepareMainSubmit", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/QuadInstance;setColor(I)V"))
    private int translucentItemColor(int color, @Local(argsOnly = true) ItemFeatureRenderer.Submit submit) {
        float opacity = noammaddons$getOpacity(submit);
        if (opacity >= 1f) return color;
        return ARGB.multiplyAlpha(color, opacity);
    }

    //~ if <26.3 '"prepareMainSubmit"' -> '"prepareFoilSubmit"'
    @ModifyVariable(method = "prepareMainSubmit", at = @At("STORE"))
    private ItemStackRenderState.FoilType hideTranslucentFoil(ItemStackRenderState.FoilType foilType, @Local(argsOnly = true) ItemFeatureRenderer.Submit submit) {
        if (noammaddons$getOpacity(submit) >= 1f) return foilType;
        return ItemStackRenderState.FoilType.NONE;
    }

    @Unique
    private static float noammaddons$getOpacity(ItemFeatureRenderer.Submit submit) {
        return ((ITranslucentEntity) (Object) submit.pose()).noammaddons$getOpacity();
    }
}
