package com.github.noamm9.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface IItemStackLayerRenderState {
    @Accessor("specialRenderer")
    SpecialModelRenderer<?> getSpecialRenderer();
}