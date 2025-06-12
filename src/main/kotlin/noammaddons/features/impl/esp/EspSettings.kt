package noammaddons.features.impl.esp

import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.EspUtils

object EspSettings: Feature("ESP settings used by the entire mod") {
    val highlightType by DropdownSetting("Highlight Type", EspUtils.ESPType.entries.map { it.displayName }, 1)
    val lineWidth by SliderSetting("Line Width", 1, 10, 0.1, 2.0)
    val phase by ToggleSetting("Phase", true)
    val fillOpacity by SliderSetting("Fill Opacity", 0, 100f, 1, 30.0)
    val outlineOpacity by SliderSetting("Outline Opacity", 0, 100f, 1, 100.0)
}