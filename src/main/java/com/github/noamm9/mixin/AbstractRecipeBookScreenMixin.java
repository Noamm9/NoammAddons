package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.HideRecipeBook;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {
    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractRecipeBookScreen;initButton()V"), cancellable = true)
    private void cancelInitButton(CallbackInfo ci) {
        if (!HideRecipeBook.INSTANCE.enabled) return;
        ci.cancel();
    }
}
