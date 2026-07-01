package com.github.noamm9.features.impl.visual

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.equalsOneOf
import net.minecraft.client.color.block.BlockTintSources
import net.minecraft.client.renderer.block.FluidModel
import net.minecraft.client.renderer.block.FluidStateModelSet
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.awt.Color

/**
 * @see com.github.noamm9.mixin.MixinFluidStateModelSet
 * @see com.github.noamm9.mixin.MixinLavaFogEnvironment
 */
@Suppress("UNNECESSARY_SAFE_CALL") // mc.levelRenderer is null at game init
object LavaToWater: Feature("Replaces lava with the water texture") {
    private val colorTint by ToggleSetting("Color Tint", false)
    private val tintColor by ColorSetting("Tint Color", Color(63, 118, 228), false).showIf { colorTint.value }
    override fun init() = configSettings.forEach { it.onChange { if (enabled) mc.levelRenderer?.allChanged() } }

    override fun toggle() {
        super.toggle()
        mc.levelRenderer?.allChanged()
    }

    @JvmStatic
    fun modelHook(self: FluidStateModelSet, state: FluidState, cir: CallbackInfoReturnable<FluidModel>) {
        if (! enabled) return
        if (! state.type.equalsOneOf(Fluids.LAVA, Fluids.FLOWING_LAVA)) return
        val waterModel = self.get(Fluids.WATER.defaultFluidState())

        if (! colorTint.value) cir.setReturnValue(waterModel)
        else cir.setReturnValue(FluidModel(
            waterModel.layer(),
            waterModel.stillMaterial(),
            waterModel.flowingMaterial(),
            waterModel.overlayMaterial(),
            BlockTintSources.constant(tintColor.value.rgb, tintColor.value.rgb)
        ))
    }
}