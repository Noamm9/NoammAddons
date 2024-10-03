package noammaddons.features.dungeons

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.events.Chat
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.DungeonUtils
import noammaddons.utils.LocationUtils.dungeonFloor
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.PlayerUtils.useDungeonClassAbility
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.dungeons.GhostPick.featureState
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.ThreadUtils.setTimeout

object AutoUlt {
	private data class UltMessage(val msg: String, val classes: Array<String>, val floor: Int)
	
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
    fun useUlt(event: Chat) {
        if (!config.autoUlt) return
        if (!inDungeons) return

        val matchingMessage = UltMessages.find {
            it.msg == event.component.unformattedText.removeFormatting() && it.floor == dungeonFloor
        } ?: return
	    
        if (matchingMessage.classes.contains(thePlayer?.clazz?.name?.toLowerCase())) {
            modMessage("Used Ultimate!")
	        if (featureState) {
		        featureState = false
                useDungeonClassAbility(true)
	            setTimeout(50) { featureState = true}
	        }
	        else useDungeonClassAbility(true)
        }
    }
}

