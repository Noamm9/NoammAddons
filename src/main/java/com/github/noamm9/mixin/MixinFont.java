package com.github.noamm9.mixin;

import com.github.noamm9.NoammAddons;
import com.github.noamm9.features.impl.dev.Cosmetics;
import com.github.noamm9.features.impl.dev.TextReplacer;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Font.class)
public class MixinFont {
    @ModifyVariable(method = "prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;", at = @At("HEAD"), argsOnly = true)
    private String onDrawString(String text) {
        if (!NoammAddons.isLoaded) return text;
        if (!Cosmetics.INSTANCE.enabled) return text;
        if (!Cosmetics.INSTANCE.getCustomNames().getValue()) return text;
        return TextReplacer.handleString(text);
    }

    @ModifyVariable(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;", at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence onDrawSequence(FormattedCharSequence text) {
        if (!NoammAddons.isLoaded) return text;
        if (!Cosmetics.INSTANCE.enabled) return text;
        if (!Cosmetics.INSTANCE.getCustomNames().getValue()) return text;
        return TextReplacer.handleCharSequence(text);
    }

    @ModifyVariable(method = "width(Ljava/lang/String;)I", at = @At("HEAD"), argsOnly = true)
    private String onWidthString(String text) {
        if (!NoammAddons.isLoaded) return text;
        if (!Cosmetics.INSTANCE.enabled) return text;
        if (!Cosmetics.INSTANCE.getCustomNames().getValue()) return text;
        return TextReplacer.handleString(text);
    }

    @ModifyVariable(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", at = @At("HEAD"), argsOnly = true)
    private FormattedText onWidthComponent(FormattedText text) {
        if (!NoammAddons.isLoaded) return text;
        if (!Cosmetics.INSTANCE.enabled) return text;
        if (!Cosmetics.INSTANCE.getCustomNames().getValue()) return text;
        if (text instanceof Component) return TextReplacer.handleComponent((Component) text);
        return text;
    }

    @ModifyVariable(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I", at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence onWidthSequence(FormattedCharSequence text) {
        if (!NoammAddons.isLoaded) return text;
        if (!Cosmetics.INSTANCE.enabled) return text;
        if (!Cosmetics.INSTANCE.getCustomNames().getValue()) return text;
        return TextReplacer.handleCharSequence(text);
    }
}