package noammaddons.features.impl.esp

import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.EspUtils

object GlobalEspSettings: Feature("ESP settings used by the entire mod") {
    val highlightType by DropdownSetting("Highlight Type", EspUtils.ESPType.highlightTypes, 1)
    val lineWidth by SliderSetting("Line Width", 1, 10, 2.0)
    val phase by ToggleSetting("Phase", true)
    val fillOpacity by SliderSetting("Fill Opacity", 0, 100f, 30.0)
    val outlineOpacity by SliderSetting("Outline Opacity", 0, 100f, 100.0)
}