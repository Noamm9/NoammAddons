package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.general.storageoverlay.StorageOverlay;
import com.github.noamm9.features.impl.general.storageoverlay.StorageOverlayScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContainerScreen.class, priority = 2000)
public class MixinContainerScreenBackground {
    @Unique
    private StorageOverlayScreen noammaddons$overlay() {
        return StorageOverlay.activeFor((ContainerScreen) (Object) this);
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void renderBgHook(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        StorageOverlayScreen overlay = noammaddons$overlay();
        if (overlay != null) {
            overlay.renderContainerOverlay(graphics, mouseX, mouseY);
            ci.cancel();
        }
    }
}
