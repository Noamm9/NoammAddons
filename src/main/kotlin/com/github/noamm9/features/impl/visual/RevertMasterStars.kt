package com.github.noamm9.features.impl.visual

import com.github.noamm9.features.Feature
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import java.util.Optional

/**
 * @see com.github.noamm9.mixin.MixinItemStack
 */
object RevertMasterStars: Feature("Reverts Master Stars to the old red star display.") {
    private val masterStarRegex = Regex("✪{5}[➊-➎]")

    @JvmStatic
    fun modifyHoverName(component: Component): Component {
        if (! enabled || ! LocationUtils.inSkyblock) return component
        return revertMasterStars(component)
    }

    private fun revertMasterStars(component: Component): Component {
        val match = masterStarRegex.find(component.string) ?: return component
        val redStars = match.range.first until match.range.first + (match.value.last() - '➊' + 1)
        val masterStar = match.range.last
        val result = Component.empty()
        var index = 0

        component.visit({ style, value ->
            value.forEach { character ->
                if (index != masterStar) {
                    val outputStyle = if (index in redStars) style.withColor(ChatFormatting.RED) else style
                    result.append(Component.literal(character.toString()).withStyle(outputStyle))
                }
                index ++
            }
            Optional.empty<String>()
        }, Style.EMPTY)
        return result
    }
}
