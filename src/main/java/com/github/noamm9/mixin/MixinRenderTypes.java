package com.github.noamm9.mixin;

import com.github.noamm9.utils.render.DepthTestGlow;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if >=26.3 {
import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.injection.Redirect;
//?}

@Mixin(RenderTypes.class)
public class MixinRenderTypes {
    @Inject(method = "outline", at = @At("RETURN"), cancellable = true)
    private static void swapToDepthTestedOutline(Identifier texture, CallbackInfoReturnable<RenderType> cir) {
        if (DepthTestGlow.active) cir.setReturnValue(DepthTestGlow.variant(cir.getReturnValue()));
    }

    //? if >=26.3 {
    @Redirect(method = "lambda$static$33", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderSetup$RenderSetupBuilder;sortOnUpload()Lnet/minecraft/client/renderer/rendertype/RenderSetup$RenderSetupBuilder;"))
    private static RenderSetup.RenderSetupBuilder preserveSeeThroughTextOrder(RenderSetup.RenderSetupBuilder builder) {
        // Like textSeeThrough, grayscale text must keep shadows before glyphs, not sort them by camera distance.
        return builder;
    }
    //?}
}
