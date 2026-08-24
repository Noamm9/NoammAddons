package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.dev.text.TextReplacer;
import com.github.noamm9.features.impl.general.ItemTooltip;
import com.github.noamm9.features.impl.misc.Tweaks;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(value = GuiGraphicsExtractor.class)
public abstract class MixinGuiGraphicsExtractor {
    @Shadow @Final private Matrix3x2fStack pose;

    @WrapMethod(method = "tooltip")
    private void onRenderTooltipPre(Font font, List<ClientTooltipComponent> lines, int xo, int yo, ClientTooltipPositioner positioner, @org.jspecify.annotations.Nullable Identifier style, Operation<Void> original) {
        TextReplacer.tooltip = true;
        if (! ItemTooltip.isScrollingEnabled()) original.call(font, lines, xo, yo, positioner, style);
        else {
            pose.pushMatrix();
            pose.translate(xo, yo);
            pose.scale(ItemTooltip.getTooltipScale().getValue().floatValue() / 100f + ItemTooltip.scaleOverride / 10f);
            pose.translate(ItemTooltip.scrollAmountX, ItemTooltip.scrollAmountY);
            pose.translate(- xo, - yo);
            original.call(font, lines, xo, yo, positioner, style);
            pose.popMatrix();
        }

        TextReplacer.tooltip = false;
    }

    @WrapOperation(
        method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;itemCooldown(Lnet/minecraft/world/item/ItemStack;II)V"
        )
    )
    private void hideItemCooldownOverlay(GuiGraphicsExtractor instance, ItemStack itemStack, int x, int y, Operation<Void> original) {
        if (Tweaks.shouldHideItemCooldownOverlay()) return;
        original.call(instance, itemStack, x, y);
    }
}