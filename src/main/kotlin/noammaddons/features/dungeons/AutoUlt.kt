package noammaddons.features.dungeons

import noammaddons.noammaddons.Companion.config
import noammaddons.events.Chat
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.DungeonUtils.Classes.*
import noammaddons.utils.LocationUtils.dungeonFloor
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.PlayerUtils.useDungeonClassAbility
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.dungeons.GhostPick.featureState
import noammaddons.utils.DungeonUtils.Classes
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.ThreadUtils.setTimeout

object AutoUlt {
	private data class UltMessage(val msg: String, val classes: List<Classes>, val floor: Int)
	
    private val UltMessages = listOf(
        UltMessage(
	        msg = "⚠ Maxor is enraged! ⚠",
	        classes = listOf(Healer, Tank),
	        floor = 7
        ),
        UltMessage(
            msg = "[BOSS] Goldor: You have done it, you destroyed the factory…",
            classes = listOf(Healer, Tank),
            floor = 7
        ),
        UltMessage(
            msg = "[BOSS] Sadan: My giants! Unleashed!",
            classes = listOf(Healer, Tank, Archer, Berserk, Mage),
            floor = 6
        ),
        UltMessage(
            msg = "[BOSS] Livid: I respect you for making it to here, but I'll be your undoing.",
            classes = listOf(Healer, Tank),
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
	    
        if (matchingMessage.classes.contains(thePlayer?.clazz)) {
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

