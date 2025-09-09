package noammaddons.utils

import kotlinx.coroutines.*
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.NoammAddons.Companion.mc
import noammaddons.NoammAddons.Companion.scope
import noammaddons.events.InventoryFullyOpenedEvent
import noammaddons.features.impl.dungeons.LeapMenu
import noammaddons.features.impl.dungeons.ReaperArmor.autoReaperArmorSlot
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.DungeonUtils.DungeonPlayer
import noammaddons.utils.DungeonUtils.leapTeammates
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.GuiUtils.currentChestName
import noammaddons.utils.GuiUtils.hideGui
import noammaddons.utils.GuiUtils.sendWindowClickPacket
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.ItemUtils.getItemIndexInHotbar
import noammaddons.utils.ItemUtils.skyblockID
import noammaddons.utils.MathUtils.Rotation
import noammaddons.utils.MathUtils.calcYawPitch
import noammaddons.utils.MathUtils.interpolateYaw
import noammaddons.utils.MathUtils.lerp
import noammaddons.utils.MathUtils.normalizePitch
import noammaddons.utils.MathUtils.normalizeYaw
import noammaddons.utils.PlayerUtils.closeScreen
import noammaddons.utils.PlayerUtils.getArmor
import noammaddons.utils.PlayerUtils.holdClick
import noammaddons.utils.PlayerUtils.rotate
import noammaddons.utils.PlayerUtils.sendRightClickAirPacket
import noammaddons.utils.PlayerUtils.swapToSlot
import noammaddons.utils.PlayerUtils.toggleSneak
import noammaddons.utils.RenderUtils.drawTitle
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.containsOneOf
import noammaddons.utils.Utils.send
import kotlin.math.abs
import kotlin.math.min


object ActionUtils {
    private var activeJob: Job? = null
    private val actionQueue = ArrayDeque<suspend () -> Unit>()
    private var currentActionName: String? = null
    fun currentAction(): String? = currentActionName

    private val rotationQueue = ArrayDeque<suspend () -> Unit>()
    var rotationJob: Job? = null

    private var LEAP_TARGET: DungeonPlayer? = null
    private var awaitingPotionBag = ""
    private var awaiting4EQ = ""

    fun rodSwap() = queueAction("Rod Swap") { rodSwapAction() }

    fun leap(leapTarget: DungeonPlayer) = queueAction("Leap") { leapAction(leapTarget) }

    fun changeMask() = queueAction("Change Mask") { changeMaskAction() }

    fun quickSwapTo(itemID: String) = queueAction("Quick Swap") { quickSwapAction(itemID) }

    fun getPotion(name: String) = queueAction("Potion") { getPotionAction(name) }

    fun reaperSwap() = queueAction("Reaper Swap") { reaperSwapAction() }

    fun leap(leapTarget: String) = queueAction("Leap") {
        leapAction(leapTeammates.find {
            it.name.lowercase() == leapTarget.lowercase()
        } ?: return@queueAction modMessage("&c$leapTarget not found!"))
    }

    private fun queueAction(actionName: String, action: suspend () -> Unit) {
        actionQueue.add {
            currentActionName = actionName
            try {
                action.invoke()
            }
            finally {
                currentActionName = null
            }
        }
        if (activeJob?.isActive != true) return processQueue()
    }

    private fun processQueue() {
        activeJob = scope.launch {
            while (actionQueue.isNotEmpty()) {
                try {
                    actionQueue.removeFirst().invoke()
                }
                catch (e: Exception) {
                    modMessage("Error during action: ${e.message}")
                }
            }
        }
    }


    private fun processRotationQueue() {
        rotationJob = scope.launch {
            while (rotationQueue.isNotEmpty()) {
                runCatching {
                    rotationQueue.removeFirst().invoke()
                }
            }
        }
    }

    private fun queueRotation(action: suspend () -> Unit) {
        rotationQueue.add(action)
        if (rotationJob?.isActive != true) processRotationQueue()
    }

    val easeInOutCubic = fun(t: Double) = if (t < 0.5) 4 * t * t * t else (t - 1) * (2 * t - 2) * (2 * t - 2) + 1

    fun rotateSmoothly(rot: Rotation, time: Long, block: suspend () -> Unit = {}) {
        queueRotation {
            val currentYaw = normalizeYaw(mc.thePlayer?.rotationYaw ?: return@queueRotation)
            val currentPitch = normalizePitch(mc.thePlayer?.rotationPitch ?: return@queueRotation)
            val targetYaw = normalizeYaw(rot.yaw)
            val targetPitch = normalizePitch(rot.pitch)
            val tolerance = 1.0f

            if (abs(currentYaw - targetYaw) <= tolerance && abs(currentPitch - targetPitch) <= tolerance) return@queueRotation block()

            val startTime = System.currentTimeMillis()

            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = if (time <= 0) 1.0 else min(elapsed.toDouble() / time, 1.0)
                if (progress >= 1.0) {
                    block()
                    break
                }

                val easedProgress = easeInOutCubic(progress).toFloat()
                val newYaw = interpolateYaw(currentYaw, targetYaw, easedProgress)
                val newPitch = lerp(currentPitch, targetPitch, easedProgress).toFloat()

                rotate(newYaw, newPitch)
            }
        }
    }

    fun rotateSmoothlyTo(vec: Vec3, time: Long, block: suspend () -> Unit = {}) {
        rotateSmoothly(calcYawPitch(vec), time, block)
    }

    suspend fun rodSwapAction() {
        if (thePlayer?.isDead == true) return
        val slotBeforeSwap = mc.thePlayer?.inventory?.currentItem ?: return
        val rodIndex = getItemIndexInHotbar("rod") ?: return modMessage("&cNo rod found in hotbar!")
        val keyState = mc.gameSettings.keyBindUseItem.isKeyDown

        if (keyState) holdClick(false)
        swapToSlot(rodIndex)
        delay(80)
        mc.thePlayer.swingItem()
        sendRightClickAirPacket()
        delay(80)
        swapToSlot(slotBeforeSwap)
        holdClick(keyState)
        delay(100)
    }

    suspend fun leapAction(leapTarget: DungeonPlayer) {
        if (thePlayer?.isDead == true) return
        if (leapTarget.isDead) return modMessage(leapTarget.name + " is dead R.I.P!")
        val leapIndex = getItemIndexInHotbar("leap") ?: return modMessage("&cNo leap found in hotbar!")

        hideGui(true) { drawTitle("&5[Leaping to &b${leapTarget.name}]", "&bPlease wait") }

        if (! inLeapMenu) {
            if (ServerPlayer.player.heldHotbarSlot != leapIndex) {
                swapToSlot(leapIndex)
                delay(80)
            }
            sendRightClickAirPacket()
            LEAP_TARGET = leapTarget
            setTimeout(5000) { LEAP_TARGET = null }

            while (LEAP_TARGET != null) delay(50)
        }

        LeapMenu.updateLeapMenu()
        LeapMenu.players.find { it?.player?.name == leapTarget.name }?.let { target ->
            modMessage("Leaping To: &e[${leapTarget.clazz.name[0]}] &a${leapTarget.name}")
            sendWindowClickPacket(target.slot, 0, 0)
            closeScreen()
            delay(500)
            hideGui(false)
        }
    }

    suspend fun changeMaskAction() {
        val sbIds = mc.thePlayer.inventory.mainInventory.mapNotNull { it?.skyblockID }
        val mask = sbIds.find { it.containsOneOf("SPIRIT_MASK", "BONZO_MASK") } ?: return
        quickSwapAction(mask)
    }

    private suspend fun quickSwapAction(itemID: String) {
        if (thePlayer?.isDead == true) return
        if (getArmor().any { it.skyblockID == itemID }) return

        C01PacketChatMessage("/eq").send()
        hideGui(true) { drawTitle("&5[Swapping]", "") }
        awaiting4EQ = itemID
        setTimeout(5000) { awaiting4EQ = "" }

        while (awaiting4EQ.isNotBlank()) delay(1)
    }

    private suspend fun getPotionAction(name: String) {
        if (mc.thePlayer?.inventory?.mainInventory?.contains(null) != true) return modMessage("&cYour inventory is full!")

        closeScreen()
        sendChatMessage("/pb")
        hideGui(true) { drawTitle("&d[Getting potion] ", "") }

        awaitingPotionBag = name
        setTimeout(5000) { awaitingPotionBag = "" }

        while (awaitingPotionBag.isNotBlank()) delay(50)
    }

    suspend fun reaperSwapAction() {
        if (thePlayer?.isDead == true) return

        C01PacketChatMessage("/wd").send()
        hideGui(true) { drawTitle("&8[Swapping armor]", "") }

        while (! inWardrobeMenu) delay(50)
        delay(250)

        val container = mc.thePlayer.openContainer.inventory
        val reaperArmorSlot: Int = autoReaperArmorSlot.value + 35
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
            SoundUtils.Pling()
            return
        }

        sendWindowClickPacket(reaperArmorSlot, 0, 0)

        delay(100)
        closeScreen()
        delay(200)

        toggleSneak(true)
        delay(100)
        toggleSneak(false)

        C01PacketChatMessage("/wd").send()
        while (! inWardrobeMenu) delay(50)
        delay(250)

        sendWindowClickPacket(reaperSwapPreviousArmorSlot, 0, 0)
        delay(100)

        closeScreen()
        hideGui(false)
        SoundUtils.Pling()
    }

    val inLeapMenu get() = currentChestName.removeFormatting().lowercase() == "spirit leap"
    val inEQMenu get() = currentChestName.removeFormatting().lowercase() == "your equipment and stats"
    val inWardrobeMenu get() = currentChestName.removeFormatting().matches(Regex("^Wardrobe \\(\\d/\\d\\)$"))
    val inPotionBag get() = currentChestName.removeFormatting().lowercase() == "potion bag"


    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        when {
            inPotionBag -> {
                if (awaitingPotionBag.isBlank()) return
                event.items.forEach { (i, item) ->
                    item ?: return@forEach
                    if (item.getItemId() != 373) return@forEach
                    if (! item.displayName.removeFormatting().lowercase().contains(awaitingPotionBag.removeFormatting().lowercase())) return@forEach

                    sendWindowClickPacket(i, 0, 1)
                    awaitingPotionBag = ""
                    setTimeout(250) {
                        closeScreen()
                        hideGui(false)
                    }
                    return
                }

                modMessage("&cNo potion found in the Potion Bag!")
                closeScreen()
                hideGui(false)
            }

            inLeapMenu -> {
                if (LEAP_TARGET == null) return
                LeapMenu.updateLeapMenu()
                LeapMenu.players.find { it?.player?.name == LEAP_TARGET?.name }?.let { target ->
                    modMessage("Leaping To: &e[${LEAP_TARGET !!.clazz.name[0]}] &a${LEAP_TARGET !!.name}")
                    sendWindowClickPacket(target.slot, 0, 0)
                    closeScreen()
                    LEAP_TARGET = null
                    setTimeout(500) { hideGui(false) }
                }
            }

            inEQMenu -> {
                if (awaiting4EQ.isBlank()) return
                val con = mc.thePlayer?.openContainer?.inventorySlots ?: return resetEQ("&cInventory didnt fully open???")

                val item = con.filter {
                    it.slotNumber in con.size - 36 until con.size
                }.find {
                    it.stack?.skyblockID?.contains(awaiting4EQ) == true
                } ?: return resetEQ("&cItem Not Found $awaiting4EQ")

                sendWindowClickPacket(item.slotNumber, 0, 0)
                resetEQ("Quick swap to ${item.stack?.displayName}")
            }
        }
    }

    private fun resetEQ(message: String? = null) {
        setTimeout(1000) { hideGui(false) }
        awaiting4EQ = ""
        message?.let { modMessage(it) }
        closeScreen()
        SoundUtils.Pling()
    }
}
