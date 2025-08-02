package noammaddons.features.impl.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.DungeonUtils
import noammaddons.utils.DungeonUtils.Classes.*
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.PlayerUtils.useDungeonClassAbility

object AutoUlt: Feature("Automatically uses your dungeon class ultimate when needed") {
    private class UltMessage(val msg: String, val classes: List<DungeonUtils.Classes>, val floor: Int)

    // todo offload to json?
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
    fun onChat(event: Chat) {
        if (! inBoss) return
        val msg = event.component.noFormatText
        val matchingMessage = UltMessages.find {
            it.msg == msg && it.floor == dungeonFloorNumber
        } ?: return

        if (thePlayer?.clazz !in matchingMessage.classes) return
        useDungeonClassAbility(ultimate = true)
        modMessage("Used Ultimate!")
    }
}