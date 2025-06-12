package noammaddons.features.impl.gui

import noammaddons.features.Feature
import noammaddons.ui.config.core.annotations.AlwaysActive
import noammaddons.ui.config.core.annotations.Dev
import noammaddons.ui.config.core.impl.ColorSetting
import noammaddons.ui.config.core.impl.DropdownSetting
import java.awt.Color

@Dev
@AlwaysActive
object ConfigGui: Feature("Customize the config menu to your liking", toggled = true) {
    init {
        enabled = true
    }

    val guiType by DropdownSetting("Gui Type", listOf("Default", "Click Gui"))
    val accentColor by ColorSetting("Accent Color", Color(58, 142, 240), false)

    override fun toggle() {}
    override fun onEnable() {}
}