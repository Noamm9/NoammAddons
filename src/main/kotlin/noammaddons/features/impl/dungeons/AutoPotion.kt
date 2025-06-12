package noammaddons.features.impl.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ActionUtils.getPotion
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.GuiUtils


object AutoPotion: Feature() {
    const val potionName = "Dungeon VII Potion"
    private val floorEnterRegex = Regex("-+\\s.+ entered.+The Catacombs, Floor [IVX]+!\\s-+")

    private val onDungonStart by ToggleSetting("On Dungeon Start", true)

    @SubscribeEvent
    fun onChat(event: Chat) {
        val msg = event.component.noFormatText
        if (msg == "You need the Cookie Buff active to use this feature!") return GuiUtils.hideGui(false)
        if (msg.matches(floorEnterRegex) && onDungonStart) {
            if (hasPotion()) return
            getPotion(potionName)
        }
    }


    private fun hasPotion(): Boolean {
        return mc.thePlayer?.inventory?.mainInventory?.any {
            it?.displayName?.removeFormatting()?.lowercase()
                ?.contains(potionName.lowercase()) ?: false
        } == true
    }
}
