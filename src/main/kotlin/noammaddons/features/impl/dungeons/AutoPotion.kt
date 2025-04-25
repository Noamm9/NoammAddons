package noammaddons.features.impl.dungeons

import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ActionUtils.getPotion
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.GuiUtils


object AutoPotion: Feature() {
    const val potionName = "Dungeon VII Potion"
    private val floorEnterRegex = Regex("-+\\s.+ entered.+The Catacombs, Floor [IVX]+!\\s-+")

    private val onDungonStart = ToggleSetting("On Dungeon Start", true)

    override fun init() {
        addSettings(onDungonStart)

        onChat(floorEnterRegex) {
            if (! onDungonStart.value) return@onChat
            if (hasPotion()) return@onChat
            getPotion(potionName)
        }

        onChat {
            if (it.value == "You need the Cookie Buff active to use this feature!") {
                GuiUtils.hideGui(false)
            }
        }
    }

    private fun hasPotion(): Boolean {
        return mc.thePlayer?.inventory?.mainInventory?.any {
            it?.displayName?.removeFormatting()?.lowercase()
                ?.contains(potionName.lowercase()) ?: false
        } == true
    }
}
