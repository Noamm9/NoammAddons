package com.github.noamm9.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiGraphicsExtractor.class)
public interface IGuiGraphicsExtractor {
    @Accessor("guiRenderState")
    GuiRenderState getGuiRenderState();
}
