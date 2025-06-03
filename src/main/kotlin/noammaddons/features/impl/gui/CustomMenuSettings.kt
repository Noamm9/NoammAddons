package noammaddons.features.impl.gui

import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.SliderSetting

object CustomMenuSettings: Feature() {
    val scale by SliderSetting("Scale", 1, 100, 1, 75)
}
