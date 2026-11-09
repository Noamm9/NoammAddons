package com.github.noamm9.mixin;

import com.github.noamm9.utils.render.DepthTestGlow;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemFeatureRenderer.class)
public class MixinItemFeatureRenderer {
    @WrapMethod(method = "prepareOutlineSubmit")
    private void beginDepthTestedGlow(ItemFeatureRenderer.Submit submit, Operation<Void> original) {
        DepthTestGlow.active = DepthTestGlow.isTagged(submit.outlineColor());
        original.call(submit);
        DepthTestGlow.active = false;
    }
}