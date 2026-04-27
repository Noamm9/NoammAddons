package com.github.noamm9.mixin;

import com.github.noamm9.NoammAddons;
import com.github.noamm9.features.impl.dev.Cosmetics;
import com.github.noamm9.features.impl.dev.text.TextReplacer;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Font.class)
public class MixinFont {
    @Unique
    private static boolean noammaddons$shouldReplace() {
        return NoammAddons.isLoaded && Cosmetics.INSTANCE.enabled && Cosmetics.INSTANCE.getCustomNames().getValue();
    }

    @ModifyVariable(method = "prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;", at = @At("HEAD"), argsOnly = true)
    private String onString(String text) {
        return noammaddons$shouldReplace() ? TextReplacer.handleString(text) : text;
    }

    @ModifyVariable(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;", at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence onCharSequence(FormattedCharSequence text) {
        return noammaddons$shouldReplace() ? TextReplacer.handleCharSequence(text) : text;
    }

    @ModifyVariable(method = "width(Ljava/lang/String;)I", at = @At("HEAD"), argsOnly = true)
    private String onWidthString(String text) {
        return noammaddons$shouldReplace() ? TextReplacer.handleString(text) : text;
    }

    @ModifyVariable(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I", at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence onWidthCharSequence(FormattedCharSequence text) {
        return noammaddons$shouldReplace() ? TextReplacer.handleCharSequence(text) : text;
    }

    @ModifyVariable(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", at = @At("HEAD"), argsOnly = true)
    private FormattedText onWidthFormattedText(FormattedText text) {
        return noammaddons$shouldReplace() ? TextReplacer.handleFormattedText(text) : text;
    }
}