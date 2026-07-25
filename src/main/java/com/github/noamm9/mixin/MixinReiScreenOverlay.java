package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.general.storageoverlay.StorageOverlay;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.noamm9.NoammAddons.mc;

/**
 * Prevents REI's entry/favorites panels from consuming scroll events while the
 * storage overlay is replacing a chest screen. The event can then continue to
 * AbstractContainerScreen, where StorageOverlayScreen handles it.
 */
@Pseudo
@Mixin(targets = "me.shedaniel.rei.impl.client.gui.ScreenOverlayImpl", remap = false)
public abstract class MixinReiScreenOverlay {
    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void noammaddons$passStorageScrollToScreen(
        double mouseX,
        double mouseY,
        double horizontalAmount,
        double verticalAmount,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (mc.screen instanceof ContainerScreen screen && StorageOverlay.activeFor(screen) != null) {
            cir.setReturnValue(false);
        }
    }
}
