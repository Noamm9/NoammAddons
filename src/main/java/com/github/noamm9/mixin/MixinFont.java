package com.github.noamm9.mixin;

import com.github.noamm9.NoammAddons;
import com.github.noamm9.features.impl.dev.Cosmetics;
import com.github.noamm9.features.impl.dev.text.TextReplacer;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Font.class)
public class MixinFont {
    @Unique
    private static boolean noammaddons$shouldReplace() {
        return NoammAddons.isLoaded && Cosmetics.INSTANCE.enabled && Cosmetics.getCustomNames().getValue() && ! TextReplacer.drawingTooltip;
    }

    @ModifyVariable(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;", at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence onDrawSequence(FormattedCharSequence text) {
        if (! noammaddons$shouldReplace()) return text;
        return TextReplacer.INSTANCE.replace(text);
    }

    @ModifyVariable(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I", at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence onWidthSequence(FormattedCharSequence text) {
        if (! noammaddons$shouldReplace()) return text;
        return TextReplacer.INSTANCE.replace(text);
    }
}