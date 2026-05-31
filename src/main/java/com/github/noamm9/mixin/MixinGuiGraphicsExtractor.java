package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.ScrollableTooltip;
import com.github.noamm9.features.impl.visual.RevertAxes;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
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
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(value = GuiGraphicsExtractor.class)
public abstract class MixinGuiGraphicsExtractor {
    @Shadow @Final private Matrix3x2fStack pose;

    @WrapMethod(method = "tooltip")
    private void onRenderTooltipPre(Font font, List<ClientTooltipComponent> lines, int xo, int yo, ClientTooltipPositioner positioner, @org.jspecify.annotations.Nullable Identifier style, Operation<Void> original) {
        if (!ScrollableTooltip.INSTANCE.enabled) original.call(font, lines, xo, yo, positioner, style);
        else {
            pose.pushMatrix();
            pose.translate(xo, yo);
            pose.scale((1 * (ScrollableTooltip.INSTANCE.getScale().getValue().floatValue() / 100f)) + ScrollableTooltip.scaleOverride / 10);
            pose.translate(ScrollableTooltip.scrollAmountX, ScrollableTooltip.scrollAmountY);
            pose.translate(-xo, -yo);
            original.call(font, lines, xo, yo, positioner, style);
            pose.popMatrix();
        }
    }

    @ModifyVariable(
        method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private ItemStack revertAxe(ItemStack itemStack) {
        return RevertAxes.shouldReplace(itemStack);
    }
}