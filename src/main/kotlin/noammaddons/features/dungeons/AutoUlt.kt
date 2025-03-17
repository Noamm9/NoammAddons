package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.features.dungeons.GhostPick.featureState
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.DungeonUtils
import noammaddons.utils.DungeonUtils.Classes.*
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.PlayerUtils.useDungeonClassAbility

object AutoUlt: Feature() {
    data class UltMessage(val msg: String, val classes: List<DungeonUtils.Classes>, val floor: Int)

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
        ),/*
        UltMessage(
            msg = "[BOSS] Storm: I should have known that I stood no chance.",
            classes = listOf(Berserk),
            floor = 7
        ),*/
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
        if (! config.autoUlt) return
        if (! inDungeon) return

        val matchingMessage = UltMessages.find {
            it.msg == event.component.noFormatText && it.floor == dungeonFloorNumber
        } ?: return

        if (matchingMessage.classes.contains(thePlayer?.clazz)) {
            modMessage("Used Ultimate!")
            if (featureState) {
                featureState = false
                useDungeonClassAbility(true)
                featureState = true
            }
            else useDungeonClassAbility(true)
        }
    }
}

