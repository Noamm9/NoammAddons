package com.github.noamm9.mixin;

import com.github.noamm9.utils.render.DepthTestGlow;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(RenderType.class)
public class MixinRenderType {
    @Inject(method = "outline", at = @At("RETURN"), cancellable = true)
    private void swapToDepthTestedOutline(CallbackInfoReturnable<Optional<RenderType>> cir) {
        if (DepthTestGlow.active) cir.setReturnValue(cir.getReturnValue().map(DepthTestGlow::variant));
    }
}