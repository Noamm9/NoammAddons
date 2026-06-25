package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.visual.PackDisabler;
import com.github.noamm9.features.impl.misc.ScrollableTooltip;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(value = GuiGraphicsExtractor.class)
public abstract class MixinGuiGraphicsExtractor {
    @Shadow @Final private Matrix3x2fStack pose;

    @WrapMethod(method = "tooltip")
    private void onRenderTooltipPre(Font font, List<ClientTooltipComponent> lines, int xo, int yo, ClientTooltipPositioner positioner, @org.jspecify.annotations.Nullable Identifier style, Operation<Void> original) {
        Identifier tooltipStyle = PackDisabler.INSTANCE.enabled && style != null && style.getNamespace().equals("hypixel_skyblock") ? null : style;
        if (!ScrollableTooltip.INSTANCE.enabled) original.call(font, lines, xo, yo, positioner, tooltipStyle);
        else {
            pose.pushMatrix();
            pose.translate(xo, yo);
            pose.scale(ScrollableTooltip.INSTANCE.getScale().getValue().floatValue() / 100f + ScrollableTooltip.scaleOverride / 10f);
            pose.translate(ScrollableTooltip.scrollAmountX, ScrollableTooltip.scrollAmountY);
            pose.translate(-xo, -yo);
            original.call(font, lines, xo, yo, positioner, tooltipStyle);
            pose.popMatrix();
        }
    }
}