package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.utils.ActionUtils.getPotion
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.PlayerUtils.Player


object AutoPotion: Feature() {
    const val potionName = "Dungeon VII Potion"
    private val floorEnterRegex = Regex("-+\\s.+ entered.+The Catacombs, Floor [IVX]+!\\s-+")

    @SubscribeEvent
    fun onDungeonStart(event: Chat) {
        if (! config.AutoPotion) return
        if (! event.component.unformattedText.removeFormatting().matches(floorEnterRegex)) return
        if (hasPotion(potionName)) return

        getPotion(potionName)
    }


    fun hasPotion(name: String): Boolean {
        return Player?.inventory?.mainInventory?.any {
            it?.displayName?.removeFormatting()?.lowercase()
                ?.contains(name.lowercase().removeFormatting()) ?: false
        } == true
    }
}
