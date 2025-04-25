package noammaddons.features.impl.misc

import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.DropdownSetting

object TimeChanger: Feature("Changes the world time") {
    @JvmStatic
    val timeChangerMode by DropdownSetting("Time", listOf("Day", "Noon", "Sunset", "Night", "Midnight", "Sunrise"))
}
