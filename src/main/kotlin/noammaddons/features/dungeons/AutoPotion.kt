package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.utils.ActionUtils.getPotion
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.GuiUtils


object AutoPotion: Feature() {
    const val potionName = "Dungeon VII Potion"
    private val floorEnterRegex = Regex("-+\\s.+ entered.+The Catacombs, Floor [IVX]+!\\s-+")

    @SubscribeEvent
    fun onDungeonStart(event: Chat) {
        val msg = event.component.noFormatText

        if (msg.matches(floorEnterRegex)) {
            if (! config.AutoPotion) return
            if (hasPotion(potionName)) return
            getPotion(potionName)
        }

        if (msg == "You need the Cookie Buff active to use this feature!") {
            GuiUtils.hideGui(false)
        }
    }


    fun hasPotion(name: String): Boolean {
        return mc.thePlayer?.inventory?.mainInventory?.any {
            it?.displayName?.removeFormatting()?.lowercase()
                ?.contains(name.lowercase().removeFormatting()) ?: false
        } == true
    }
}
