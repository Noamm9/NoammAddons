package com.github.noamm9.utils.render

import com.github.noamm9.mixin.IRenderSetup
import com.github.noamm9.mixin.IRenderType
import com.github.noamm9.mixin.ITextureBinding
import net.minecraft.client.renderer.feature.ItemFeatureRenderer
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.resources.Identifier
import java.util.*

object TranslucentEntity {
    @JvmField var opacity = 1f

    private val glintTextures = setOf(ItemFeatureRenderer.ENCHANTED_GLINT_ARMOR, ItemFeatureRenderer.ENCHANTED_GLINT_ITEM)
    private val variants = IdentityHashMap<RenderType, RenderType>()

    @JvmStatic fun isActive() = opacity < 1f
    @JvmStatic fun isGlint(renderType: RenderType) = texture(renderType) in glintTextures

    @JvmStatic
    fun translucent(renderType: RenderType): RenderType = variants.getOrPut(renderType) {
        if (renderType.hasBlending() || isGlint(renderType)) return@getOrPut renderType
        texture(renderType)?.let(RenderTypes::entityTranslucent) ?: renderType
    }

    private fun texture(renderType: RenderType): Identifier? {
        val binding = ((renderType as IRenderType).state as IRenderSetup).textures["Sampler0"] ?: return null
        return (binding as ITextureBinding).location
    }
}
