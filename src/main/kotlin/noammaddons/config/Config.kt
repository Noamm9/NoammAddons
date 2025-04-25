package noammaddons.config

import gg.essential.universal.UDesktop
import gg.essential.vigilance.Vigilant
import gg.essential.vigilance.data.Property
import gg.essential.vigilance.data.PropertyType
import noammaddons.noammaddons.Companion.FULL_PREFIX
import noammaddons.noammaddons.Companion.MOD_NAME
import noammaddons.noammaddons.Companion.MOD_VERSION
import noammaddons.utils.ChatUtils.addColor
import java.io.File
import java.net.URI


object Config: Vigilant(
    File("./config/$MOD_NAME/config.toml"),
    "$FULL_PREFIX&r &6($MOD_VERSION)".addColor(),
) {
    private const val EMPTY_CATEGORY = ""

    @Property(
        type = PropertyType.BUTTON,
        name = "Join my Discord Server",
        description = "Feel free to join my Discord Server.",
        category = EMPTY_CATEGORY,
        placeholder = "CLICK"
    )
    fun openDiscordLink() {
        UDesktop.browse(URI("https://discord.gg/pj9mQGxMxB"))
    }

    private const val EDIT_HUD_CONFIG_DESCRIPTION =
        "Opens the Hud Edit GUI\n\n" +
                "Left Click + Drag: Move Element around the screen\n" +
                "Left Click + Scroll Wheel: Control the scale"

    private const val DEV_MODE_DESCRIPTION =
        "Forces all features to enable, even if you are not on skyblock.\n\n" +
                "enables console logging and disables a few safety checks\n" +
                "Q: Why is this a thing?\n" +
                "A: So I can properly test features in the mod without needing to be in skyblock\n\n" +
                "DONT USE IT IF U ARE NOT ME, CAN GET YOU BANNED!\n\n" +
                "[R.I.P] FININ1, NoamIsSad"
}