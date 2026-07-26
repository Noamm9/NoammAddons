package com.github.noamm9.mixin;

import com.github.noamm9.init.ModCompatibility;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "me.shedaniel.rei.impl.client.gui.ScreenOverlayImpl", remap = false)
public abstract class MixinRei {
    @Dynamic
    @Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z", at = @At("HEAD"), cancellable = true)
    public void cancelClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (ModCompatibility.isCustomMenuActive()) cir.setReturnValue(false);
    }

    @Dynamic
    @Inject(method = "mouseReleased(Lnet/minecraft/client/input/MouseButtonEvent;)Z", at = @At("HEAD"), cancellable = true)
    public void cancelRelease(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (ModCompatibility.isCustomMenuActive()) cir.setReturnValue(false);
    }

    @Dynamic
    @Inject(method = "mouseDragged(Lnet/minecraft/client/input/MouseButtonEvent;DD)Z", at = @At("HEAD"), cancellable = true)
    public void cancelDrag(MouseButtonEvent event, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        if (ModCompatibility.isCustomMenuActive()) cir.setReturnValue(false);
    }

    @Dynamic
    @Inject(method = "mouseScrolled(DDDD)Z", at = @At("HEAD"), cancellable = true)
    public void cancelScroll(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        if (ModCompatibility.isCustomMenuActive()) cir.setReturnValue(false);
    }

    @Dynamic
    @Inject(method = "keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z", at = @At("HEAD"), cancellable = true)
    public void cancelKeyPress(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (ModCompatibility.isCustomMenuActive()) cir.setReturnValue(false);
    }

    @Dynamic
    @Inject(method = "keyReleased(Lnet/minecraft/client/input/KeyEvent;)Z", at = @At("HEAD"), cancellable = true)
    public void cancelKeyRelease(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (ModCompatibility.isCustomMenuActive()) cir.setReturnValue(false);
    }

    @Dynamic
    @Inject(method = "charTyped(Lnet/minecraft/client/input/CharacterEvent;)Z", at = @At("HEAD"), cancellable = true)
    public void cancelCharTyped(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (ModCompatibility.isCustomMenuActive()) cir.setReturnValue(false);
    }
}