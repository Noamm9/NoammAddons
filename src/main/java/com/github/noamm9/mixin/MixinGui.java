package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.general.storageoverlay.StorageOverlay;
import com.github.noamm9.interfaces.IGui;
import com.github.noamm9.ui.notification.NotificationManager;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinGui implements IGui {
    @Shadow private @Nullable Screen screen;

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void onSetScreen(Screen screen, CallbackInfo ci, @Local(argsOnly = true) LocalRef<Screen> screenRef) {
        if (!StorageOverlay.INSTANCE.enabled) return;
        var newScreen = StorageOverlay.onScreenChange(this.screen, screen);
        if (newScreen != null) screenRef.set(newScreen);
    }

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;applyCursor(Lcom/mojang/blaze3d/platform/Window;)V"))
    public void onExtractGui(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci, @Local GuiGraphicsExtractor graphics) {
        NotificationManager.render(graphics);
    }

    @Override
    public void replaceScreen(Screen screen) {
        this.screen = screen;
    }
}