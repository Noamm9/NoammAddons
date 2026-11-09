package com.github.noamm9.utils.render

import com.github.noamm9.NoammAddons
import com.github.noamm9.compat.*
import com.github.noamm9.interfaces.IGlowingEntity
import com.github.noamm9.mixin.*
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.rendertype.*
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.Entity
import java.util.*

// marks entity's glow color with custom alpha so we can know if it should have depth or not
object DepthTestGlow {
    private const val TAG = 0xFE

    @JvmField var active = false

    private val cullPipeline by lazy { pipeline("cull", true) }
    private val noCullPipeline by lazy { pipeline("no_cull", false) }
    private val variants = IdentityHashMap<RenderType, RenderType>()

    @JvmStatic
    fun shouldDepthTest(entity: Entity) = ! NoammAddons.isCheat && (entity as IGlowingEntity).`noammaddons$isGlowing`() && ! entity.isCurrentlyGlowing

    @JvmStatic fun tag(color: Int) = color and 0xFFFFFF or (TAG shl 24)
    @JvmStatic fun isTagged(color: Int) = color ushr 24 == TAG
    @JvmStatic fun variant(outline: RenderType): RenderType = variants.getOrPut(outline) { createVariant(outline) }

    @Suppress("KotlinConstantConditions")
    private fun createVariant(outline: RenderType): RenderType {
        val binding = ((outline as IRenderType).state as IRenderSetup).textures.values.first() as ITextureBinding
        val pipeline = if (outline.pipeline() == RenderPipelines.OUTLINE_CULL) cullPipeline else noCullPipeline

        return IRenderType.invokeCreate(
            "NOAMM_OUTLINE_DEPTH",
            RenderSetup.builder(pipeline).apply {
                setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING) // stops armor from drawing over the glow
                setOutline(RenderSetup.OutlineProperty.IS_OUTLINE)
                withTexture("Sampler0", binding.location)
                withOutlineTarget()
            }.createRenderSetup()
        )
    }

    private fun pipeline(name: String, cull: Boolean) = RenderPipeline.builder(IRenderPipelines.getOutlineSnippet()).apply {
        withLocation(Identifier.fromNamespaceAndPath(NoammAddons.MOD_ID, "pipeline/outline_depth_$name"))
        withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        withCull(cull)
    }.build()
}