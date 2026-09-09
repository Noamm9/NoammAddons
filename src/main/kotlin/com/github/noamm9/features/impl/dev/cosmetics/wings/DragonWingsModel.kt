package com.github.noamm9.features.impl.dev.cosmetics.wings

import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class DragonWingsModel(root: ModelPart): EntityModel<AvatarRenderState>(root) {
    private val rightWing = root.getChild("right_wing")
    private val leftWing = root.getChild("left_wing")
    private val rightTip = rightWing.getChild("tip")
    private val leftTip = leftWing.getChild("tip")

    override fun setupAnim(state: AvatarRenderState) {
        super.setupAnim(state)
        val phase = state.ageInTicks % 30f / 30f * (PI.toFloat() * 2f)
        rightWing.xRot = - 80f * PI.toFloat() / 180f - cos(phase) * 0.2f
        rightWing.yRot = 20f * PI.toFloat() / 180f + sin(phase) * 0.4f
        rightWing.zRot = 20f * PI.toFloat() / 180f
        rightTip.zRot = - (sin(phase + 2f) + 0.5f) * 0.75f
        leftWing.xRot = rightWing.xRot
        leftWing.yRot = - rightWing.yRot
        leftWing.zRot = - rightWing.zRot
        leftTip.zRot = - rightTip.zRot
    }

    companion object {
        fun create(): DragonWingsModel {
            val mesh = MeshDefinition()
            for (left in listOf(false, true)) {
                val boxX = if (left) 0f else - 56f
                val wing = mesh.root.addOrReplaceChild(
                    if (left) "left_wing" else "right_wing",
                    CubeListBuilder.create().mirror(left)
                        .texOffs(112, 88).addBox(boxX, - 4f, - 4f, 56f, 8f, 8f)
                        .texOffs(- 56, 88).addBox(boxX, 0f, 2f, 56f, 0f, 56f),
                    PartPose.offset(if (left) 12f else - 12f, 5f, 2f)
                )
                wing.addOrReplaceChild(
                    "tip",
                    CubeListBuilder.create().mirror(left)
                        .texOffs(112, 136).addBox(boxX, - 2f, - 2f, 56f, 4f, 4f)
                        .texOffs(- 56, 144).addBox(boxX, 0f, 2f, 56f, 0f, 56f),
                    PartPose.offset(if (left) 56f else - 56f, 0f, 0f)
                )
            }
            return DragonWingsModel(LayerDefinition.create(mesh, 256, 256).bakeRoot())
        }
    }
}
