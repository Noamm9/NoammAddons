package noammaddons.features.impl.misc

import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting

object FullBlock: Feature("Changes the hitbox of certain blocks to be easier to click") {
    @JvmField
    val skull = ToggleSetting("Skull").register1()

    @JvmField
    val button = ToggleSetting("Button").register1()

    @JvmField
    val lever = ToggleSetting("Lever").register1()

    @JvmField
    val mushroom = ToggleSetting("Mushroom").register1()
}