package noammaddons.features.impl

import gg.essential.api.EssentialAPI
import net.minecraft.client.gui.GuiScreen
import noammaddons.features.Feature
import noammaddons.features.FeatureManager
import noammaddons.ui.config.core.annotations.Dev
import noammaddons.ui.config.core.impl.ButtonSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils

@Dev
object DevOptions: Feature() {
    @JvmStatic
    val devMode by ToggleSetting("Dev Mode")

    @JvmStatic
    val copyFeatureList by ButtonSetting("Copy Feature List") {
        GuiScreen.setClipboardString(FeatureManager.createFeatureList())
        ChatUtils.Alert(message = "Copied all feaures to clipboard")
    }

    @JvmStatic
    val updateChecker by ToggleSetting("Update Checker", true)

    @JvmStatic
    val clientBranding by ToggleSetting("Client Branding")

    @JvmStatic
    val isDev get() = EssentialAPI.getMinecraftUtil().isDevelopment() || devMode

    @JvmStatic
    val trackTitles by ToggleSetting("Track Titles")

    @JvmStatic
    val printBlockCoords by ToggleSetting("Print Block Coords")
}
