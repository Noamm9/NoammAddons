package com.github.noamm9.mixin;

import com.github.noamm9.event.EventBus;
import com.github.noamm9.event.impl.ScreenEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "renderWithTooltipAndSubtitles", at = @At("HEAD"), cancellable = true)
    private void onRenderPre(GuiGraphics context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (EventBus.post(new ScreenEvent.PreRender((Screen) (Object) this, context, mouseX, mouseY))) ci.cancel();
    }

    @Inject(method = "renderWithTooltipAndSubtitles", at = @At("TAIL"))
    private void onRenderPost(GuiGraphics context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        EventBus.post(new ScreenEvent.PostRender((Screen) (Object) this, context, mouseX, mouseY));
    }
}
