package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.floor7.terminals.TerminalListener;
import com.github.noamm9.features.impl.general.storageoverlay.StorageOverlay;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.github.noamm9.NoammAddons.mc;

@Pseudo
@Mixin(targets = "mezz.jei.fabric.startup.EventRegistration", remap = false)
public abstract class MixinJei {
    @Dynamic
    @Inject(method = "registerScreenEvents", at = @At("HEAD"), cancellable = true)
    public void cancelEventsInTerm(CallbackInfo ci) {
        var screen = mc.screen instanceof ContainerScreen ? ((ContainerScreen) mc.screen) : null;
        if (screen == null) return;
        if (TerminalListener.inTerm || StorageOverlay.activeFor(screen) != null) ci.cancel();
    }
}