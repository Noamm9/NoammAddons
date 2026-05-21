package com.github.noamm9.features.impl.visual

import com.github.noamm9.features.Feature
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids

object LavaToWater: Feature("Replaces lava with the water texture and water fog (resource-pack aware).") {
    override fun init() {
        val origStill = FluidRenderHandlerRegistry.INSTANCE.get(Fluids.LAVA)
        val origFlowing = FluidRenderHandlerRegistry.INSTANCE.get(Fluids.FLOWING_LAVA)
        FluidRenderHandlerRegistry.INSTANCE.register(Fluids.LAVA, makeHandler(origStill))
        FluidRenderHandlerRegistry.INSTANCE.register(Fluids.FLOWING_LAVA, makeHandler(origFlowing))
    }

    override fun onEnable() {
        super.onEnable()
        if (mc.level != null) mc.levelRenderer.allChanged()
    }

    override fun onDisable() {
        super.onDisable()
        if (mc.level != null) mc.levelRenderer.allChanged()
    }

    private fun makeHandler(originalHandler: FluidRenderHandler?) = object: FluidRenderHandler {
        override fun getFluidSprites(view: BlockAndTintGetter?, pos: BlockPos?, state: FluidState): Array<TextureAtlasSprite> {
            val fallback = originalHandler?.getFluidSprites(view, pos, state) ?: arrayOf()
            if (! enabled) return fallback
            return FluidRenderHandlerRegistry.INSTANCE.get(Fluids.WATER)?.getFluidSprites(view, pos, state) ?: fallback
        }

        override fun getFluidColor(view: BlockAndTintGetter?, pos: BlockPos?, state: FluidState): Int {
            if (! enabled) return originalHandler?.getFluidColor(view, pos, state) ?: - 1
            return FluidRenderHandlerRegistry.INSTANCE.get(Fluids.WATER)?.getFluidColor(view, pos, state) ?: - 1
        }
    }
}