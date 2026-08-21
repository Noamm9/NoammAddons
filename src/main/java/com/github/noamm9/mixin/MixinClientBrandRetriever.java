package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.dev.ModHider;
import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientBrandRetriever.class)
public class MixinClientBrandRetriever {
    @Inject(method = "getClientModName", at = @At("HEAD"), remap = false, cancellable = true)
    private static void spoofClientBrand(CallbackInfoReturnable<String> cir) {
        String brand = ModHider.spoofedBrand();
        if (brand != null) cir.setReturnValue(brand);
    }
}
