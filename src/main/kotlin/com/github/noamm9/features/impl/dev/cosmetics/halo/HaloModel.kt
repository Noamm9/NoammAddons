package com.github.noamm9.features.impl.dev.cosmetics.halo

import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.renderer.entity.state.AvatarRenderState

class HaloModel(root: ModelPart): EntityModel<AvatarRenderState>(root) {
    companion object {
        fun create(): HaloModel {
            val builder = CubeListBuilder.create()
            haloPixels.forEach { (px, pz) -> builder.addBox(px.toFloat(), 0f, pz.toFloat(), 1f, 1f, 1f) }
            val haloPart = ModelPart(builder.cubes.map { it.bake(16, 16) }, emptyMap())
            return HaloModel(ModelPart(emptyList(), mapOf("halo" to haloPart)))
        }

        private val haloPixels = listOf(
            - 2 to - 6, 1 to 5, 0 to 5, - 1 to 5, - 2 to 5, - 2 to 4, - 3 to 4, - 4 to 4, - 4 to 3, - 5 to 3, - 5 to 2, - 5 to 1,
            - 6 to 1, - 6 to - 2, - 6 to - 1, 1 to 4, 2 to 4, 3 to 4, 3 to 3, 4 to 3, 4 to 2, 4 to 1, 5 to 1, 5 to 0, 5 to - 1,
            5 to - 2, - 6 to 0, - 5 to - 2, - 5 to - 3, - 5 to - 4, - 4 to - 4, - 4 to - 5, - 3 to - 5, - 2 to - 5, 4 to - 2,
            4 to - 3, 4 to - 4, 3 to - 4, 3 to - 5, 2 to - 5, 1 to - 5, 1 to - 6, 0 to - 6, - 1 to - 6
        )
    }
}