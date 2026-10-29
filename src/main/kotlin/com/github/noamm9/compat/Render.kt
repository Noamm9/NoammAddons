package com.github.noamm9.compat

import com.mojang.blaze3d.platform.TextInputManager
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.Model
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.OrderedSubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.world.InteractionHand
import org.joml.Quaternionfc

//~ if <26.3 '144' -> 'com.github.noamm9.NoammAddons.mc.window.refreshRate'
val refreshRate get() = 144

//~ if <26.3 '= rotate(quaternion)' -> '= mulPose(quaternion)'
fun PoseStack.rotateCompat(quaternion: Quaternionfc) = rotate(quaternion)

//~ if <26.3 '(owner, focused)' -> '(focused)'
fun TextInputManager.onFocusChange(owner: Any, focused: Boolean) = onTextInputFocusChange(owner, focused)

fun <S: Any> OrderedSubmitNodeCollector.submitModelCompat(
    model: Model<in S>,
    state: S,
    poseStack: PoseStack,
    renderType: RenderType,
    light: Int, overlay: Int,
    tintColor: Int, outlineColor: Int
) =
    //~ if <26.3 'null, outlineColor)' -> 'null, outlineColor, null)'
    submitModel(model, state, poseStack, renderType, light, overlay, tintColor, null, outlineColor)

fun LocalPlayer.swingMainHand() {
    //? if <26.3 {
    /*if (swinging && swingTime >= 0) return
    swingingArm = InteractionHand.MAIN_HAND
    swingTime = - 1
    swinging = true
    *///? } else {
    swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false)
    //? }
}