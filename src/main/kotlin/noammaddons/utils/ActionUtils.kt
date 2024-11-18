package noammaddons.utils

import kotlinx.coroutines.*
import noammaddons.features.gui.Menus.impl.CustomWardrobeMenu.inWardrobeMenu
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.noammaddons.Companion.scope
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.DungeonUtils.leapTeammates
import noammaddons.utils.GuiUtils.currentChestName
import noammaddons.utils.GuiUtils.hideGui
import noammaddons.utils.GuiUtils.isInGui
import noammaddons.utils.GuiUtils.sendWindowClickPacket
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.ItemUtils.getItemIndexInHotbar
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.PlayerUtils.closeScreen
import noammaddons.utils.PlayerUtils.holdClick
import noammaddons.utils.PlayerUtils.sendRightClickAirPacket
import noammaddons.utils.PlayerUtils.swapToSlot
import noammaddons.utils.PlayerUtils.toggleSneak
import noammaddons.utils.RenderUtils.drawTitle
import noammaddons.utils.SoundUtils.Pling
import noammaddons.utils.Utils.containsOneOf


@OptIn(DelicateCoroutinesApi::class)
object ActionUtils {
    private var activeJob: Job? = null
    private val actionQueue = ArrayDeque<suspend () -> Unit>()
    val isActive get() = activeJob?.isActive == true

    fun rodSwap() = queueAction { rodSwapAction() }

    fun leap(leapTarget: DungeonUtils.DungeonPlayer) = queueAction { leapAction(leapTarget) }

    fun changeMask() = queueAction { changeMaskAction() }

    fun getPotion(name: String) = queueAction { getPotionAction(name) }

    fun reaperSwap() = queueAction { reaperSwapAction() }

    fun leap(leapTarget: String) = queueAction {
        leapAction(leapTeammates.find {
            it.name.lowercase() == leapTarget.lowercase()
        } ?: return@queueAction modMessage("&c$leapTarget not found!"))
    }


    /**
     * Adds the action to the queue and ensures it's executed in order.
     */
    private fun queueAction(action: suspend () -> Unit) {
        actionQueue.add(action)
        if (activeJob == null || ! activeJob !!.isActive) processQueue()
    }

    /**
     * Processes the action queue, executing one action at a time.
     */
    private fun processQueue() {
        activeJob = scope.launch {
            while (actionQueue.isNotEmpty()) {
                val nextAction = actionQueue.removeFirst()
                try {
                    nextAction.invoke()
                }
                catch (e: Exception) {
                    modMessage("Error during action: ${e.message}")
                }
            }
        }
    }

    private suspend fun rodSwapAction() {
        val slotBeforeSwap = Player?.inventory?.currentItem ?: return
        val rodIndex = getItemIndexInHotbar("rod") ?: return modMessage("&cNo rod found in hotbar!")
        val keyState = mc.gameSettings.keyBindUseItem.isKeyDown

        if (keyState) holdClick(false)
        swapToSlot(rodIndex)
        delay(80)
        sendRightClickAirPacket()
        delay(80)
        swapToSlot(slotBeforeSwap)
        holdClick(keyState)
        delay(100)
    }

    private suspend fun leapAction(leapTarget: DungeonUtils.DungeonPlayer) {
        if (leapTeammates.isEmpty()) return modMessage("No one alive to leap GG!")
        if (leapTarget.isDead) return modMessage(leapTarget.name + " is dead R.I.P!")
        val leapIndex = getItemIndexInHotbar("leap") ?: return modMessage("&cNo leap found in hotbar!")

        if (! inLeapMenu) {
            holdClick(false)
            swapToSlot(leapIndex)
            delay(80)
            sendRightClickAirPacket()
            while (! inLeapMenu) delay(20)
        }

        val container = Player?.openContainer?.inventory ?: return
        for (i in 0 until container.size - 36) {
            val item = container[i] ?: continue
            if (item.getItemId() == 160) continue

            leapTeammates.forEach {
                if (it.clazz != leapTarget.clazz) return@forEach
                if (leapTarget.name != item.displayName.removeFormatting()) return@forEach

                sendWindowClickPacket(i, 0, 0)
                closeScreen()
                return
            }
        }
        modMessage("No one alive to leap GG!")
        closeScreen()
    }

    private suspend fun changeMaskAction() {
        sendChatMessage("/eq")
        hideGui(true) { drawTitle("&5[Swapping mask...]", "&bPlease wait") }

        while (! inEQMenu) delay(50)

        delay(250)
        val con = Player?.openContainer?.inventorySlots ?: return


        val mask = con.filter {
            it.slotNumber in con.size - 36 until con.size
        }.find { it.stack?.SkyblockID?.containsOneOf("SPIRIT_MASK", "BONZO_MASK") == true }


        if (mask == null) {
            modMessage("&cNo mask found!")
            closeScreen()
            hideGui(false)
            Pling.start()
            return
        }

        modMessage("Quick swap to ${mask.stack?.displayName}")
        sendWindowClickPacket(mask.slotNumber, 0, 0)
        closeScreen()
        Pling.start()
        delay(250)
        hideGui(false)
    }

    private suspend fun getPotionAction(name: String) {
        if (Player?.inventory?.mainInventory?.contains(null) != true) return modMessage("&cYour inventory is full!")

        closeScreen()
        sendChatMessage(config.AutoPotionCommand.removeFormatting().lowercase())
        hideGui(true) { drawTitle("&d[Getting potion...] ", "&bPlease wait") }

        while (! isInGui()) delay(50)
        delay(250)

        val con = Player?.openContainer?.inventory ?: return

        for (i in 0 until con.size - 36) {
            val item = con[i] ?: continue
            if (item.getItemId() != 373) continue
            if (! item.displayName.removeFormatting().lowercase().contains(name.removeFormatting().lowercase())) continue

            sendWindowClickPacket(i, 0, 1)
            delay(250)
            closeScreen()
            hideGui(false)
            return
        }

        modMessage("&cNo potion found in the Potion Bag!")
        closeScreen()
        hideGui(false)
        return
    }

    suspend fun reaperSwapAction() {
        closeScreen()
        sendChatMessage("/wd")
        hideGui(true) { drawTitle("&8[Swapping armor...] ", "&bPlease wait") }

        while (! inWardrobeMenu) delay(50)
        delay(250)

        val container = Player !!.openContainer.inventory
        val reaperArmorSlot: Int = config.AutoReaperArmorSlot + 35
        var reaperSwapPreviousArmorSlot = 0

        for (i in 35 until 45) {
            val item = container[i] ?: continue
            if (item.getItemId() == 351 && item.metadata == 10) {
                reaperSwapPreviousArmorSlot = i
            }
        }

        if (reaperSwapPreviousArmorSlot == 0) {
            modMessage("&a[Ras] &cPrevious Armor Slot not found")
            closeScreen()
            hideGui(false)
            Pling.start()
            return
        }

        sendWindowClickPacket(reaperArmorSlot, 0, 0)

        delay(100)
        closeScreen()
        delay(200)

        toggleSneak(true)
        delay(100)
        toggleSneak(false)

        sendChatMessage("/wd")
        while (! inWardrobeMenu) delay(50)
        delay(250)

        sendWindowClickPacket(reaperSwapPreviousArmorSlot, 0, 0)
        delay(100)

        closeScreen()
        hideGui(false)
        Pling.start()
    }

    private val inLeapMenu get() = currentChestName.removeFormatting().lowercase() == "spirit leap"
    private val inEQMenu get() = currentChestName.removeFormatting().lowercase() == "your equipment and stats"
}
