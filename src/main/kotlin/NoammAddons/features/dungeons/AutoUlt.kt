package NoammAddons.features.dungeons

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.ChatUtils
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.DungeonUtils
import NoammAddons.utils.LocationUtils.dungeonFloor
import NoammAddons.utils.LocationUtils.inDungeons
import NoammAddons.utils.PlayerUtils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object AutoUlt {
    private val UltMessages = listOf(
        UltMessage(
            msg = "⚠ Maxor is enraged! ⚠",
            classes = arrayOf("healer", "tank"),
            floor = 7
        ),
        UltMessage(
            msg = "[BOSS] Goldor: You have done it, you destroyed the factory…",
            classes = arrayOf("healer", "tank"),
            floor = 7
        ),
        UltMessage(
            msg = "[BOSS] Sadan: My giants! Unleashed!",
            classes = arrayOf("healer", "tank", "archer", "berserk", "mage"),
            floor = 6
        ),
        UltMessage(
            msg = "[BOSS] Livid: I respect you for making it to here, but I'll be your undoing.",
            classes = arrayOf("healer", "tank"),
            floor = 5
        )
    )

    @SubscribeEvent
    fun useUlt(event: ClientChatReceivedEvent) {
        if (!config.autoUlt) return
        if (!inDungeons) return
        if (event.type.toInt() != 0) return

        val matchingMessage = UltMessages.find {
            it.msg == event.message.unformattedText.removeFormatting() && it.floor == dungeonFloor
        } ?: return

        val playerClass = DungeonUtils.dungeonTeammates.find {
            it.name == mc.thePlayer.displayNameString
        }?.clazz?.name ?: return

        if (matchingMessage.classes.contains(playerClass.toLowerCase())) {
            ChatUtils.modMessage("Used Ultimate!")
            PlayerUtils.useDungeonClassAbility(true)
        }
    }

    private data class UltMessage(val msg: String, val classes: Array<String>, val floor: Int)
}

