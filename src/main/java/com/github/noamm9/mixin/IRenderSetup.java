package com.github.noamm9.mixin;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(RenderSetup.class)
public interface IRenderSetup {
    @Accessor("textures")
    Map<String, ?> getTextures();
}