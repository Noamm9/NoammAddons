package noammaddons.features.impl.dungeons

import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.DungeonUtils
import noammaddons.utils.DungeonUtils.Classes.*
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.PlayerUtils.useDungeonClassAbility

object AutoUlt: Feature("Automatically uses your dungeon class ultimate when needed") {
    data class UltMessage(val msg: String, val classes: List<DungeonUtils.Classes>, val floor: Int)

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

    override fun init() {
        onChat { match ->
            if (! inBoss) return@onChat
            val matchingMessage = UltMessages.find { it.msg == match.value && it.floor == dungeonFloorNumber } ?: return@onChat
            if (thePlayer?.clazz !in matchingMessage.classes) return@onChat
            useDungeonClassAbility(true)
            modMessage("Used Ultimate!")
        }
    }
}