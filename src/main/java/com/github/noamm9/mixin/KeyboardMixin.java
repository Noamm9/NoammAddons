package com.github.noamm9.mixin;

import com.github.noamm9.event.EventBus;
import com.github.noamm9.event.impl.KeyboardEvent;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void onKey(long l, int i, KeyEvent keyEvent, CallbackInfo ci) {
        if (keyEvent.key() == GLFW.GLFW_KEY_UNKNOWN) return;
        if (EventBus.post(new KeyboardEvent.KeyPressed(keyEvent))) {
            ci.cancel();
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onChar(long window, CharacterEvent event, CallbackInfo ci) {
        if (EventBus.post(new KeyboardEvent.CharTyped(event))) {
            ci.cancel();
        }
    }
}