package com.github.noamm9.mixin;

import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//~ if <26.3 'renderpearl.api.pipeline' -> 'blaze3d.pipeline'
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

@Mixin(RenderPipelines.class)
public interface IRenderPipelines {
    @Accessor("OUTLINE_SNIPPET")
    static RenderPipeline.Snippet getOutlineSnippet() {
        throw new AssertionError();
    }
}