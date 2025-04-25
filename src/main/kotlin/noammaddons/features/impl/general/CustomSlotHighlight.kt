package noammaddons.features.impl.general

import gg.essential.elementa.utils.withAlpha
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ColorSetting
import noammaddons.utils.Utils.favoriteColor

object CustomSlotHighlight: Feature("Allows to change the color of the slot highlighting in the inventory") {
    @JvmStatic
    val color by ColorSetting("Color", favoriteColor.withAlpha(100))
}
