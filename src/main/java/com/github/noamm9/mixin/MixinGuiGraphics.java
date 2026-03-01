package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.ScrollableTooltip;
import com.github.noamm9.features.impl.visual.RevertAxes;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;
import java.util.Objects;

@Mixin(value = GuiGraphics.class)
public abstract class MixinGuiGraphics {
    @Shadow @Final private Matrix3x2fStack pose;

    @WrapMethod(method = "renderTooltip")
    private void onRenderTooltipPre(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, @Nullable ResourceLocation background, Operation<Void> original) {
        if (!ScrollableTooltip.INSTANCE.enabled) original.call(font, components, x, y, positioner, background);
        else {
            pose.pushMatrix();
            pose.translate(x, y);
            pose.scale((1 * (ScrollableTooltip.INSTANCE.getScale().getValue().floatValue() / 100f)) + ScrollableTooltip.scaleOverride / 10);
            pose.translate(ScrollableTooltip.scrollAmountX, ScrollableTooltip.scrollAmountY);
            pose.translate(-x, -y);
            original.call(font, components, x, y, positioner, background);
            pose.popMatrix();
        }
    }

    @ModifyVariable(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At("HEAD"), argsOnly = true)
    private ItemStack revertAxe(ItemStack original) {
        if (original == null || original.isEmpty()) return original;
        ItemStack replacement = RevertAxes.shouldReplace(original);
        return Objects.requireNonNullElse(replacement, original);
    }
}