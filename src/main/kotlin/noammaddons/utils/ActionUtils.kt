package noammaddons.utils

import kotlinx.coroutines.*
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.InventoryFullyOpenedEvent
import noammaddons.features.gui.Menus.impl.CustomSpiritLeapMenu
import noammaddons.features.hud.MaskTimers
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.noammaddons.Companion.scope
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.DungeonUtils.DungeonPlayer
import noammaddons.utils.DungeonUtils.leapTeammates
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.GuiUtils.currentChestName
import noammaddons.utils.GuiUtils.hideGui
import noammaddons.utils.GuiUtils.sendWindowClickPacket
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.ItemUtils.getItemIndexInHotbar
import noammaddons.utils.MathUtils.Rotation
import noammaddons.utils.MathUtils.calcYawPitch
import noammaddons.utils.MathUtils.interpolate
import noammaddons.utils.MathUtils.interpolateYaw
import noammaddons.utils.MathUtils.normalizePitch
import noammaddons.utils.MathUtils.normalizeYaw
import noammaddons.utils.PlayerUtils.closeScreen
import noammaddons.utils.PlayerUtils.getHelmet
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

    private var INTERNAL_LEAP_TARGET: DungeonPlayer? = null
    private var awaiting4EQ = false
    private var awaitingPotionBag = ""

    fun rodSwap() = queueAction("Rod Swap") { rodSwapAction() }

    fun leap(leapTarget: DungeonPlayer) = queueAction("Leap") { leapAction(leapTarget) }

    fun changeMask() = queueAction("Change Mask") { changeMaskAction() }

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
                try {
                    rotationQueue.removeFirst().invoke()
                }
                catch (e: Exception) {
                    modMessage("Error during Rotation: ${e.message}")
                }
            }
        }
    }

    private fun queueRotation(action: suspend () -> Unit) {
        rotationQueue.add(action)
        if (rotationJob?.isActive != true) processRotationQueue()
    }

    private fun easeInOutCubic(t: Double): Double = if (t < 0.5) 4 * t * t * t else (t - 1) * (2 * t - 2) * (2 * t - 2) + 1

    fun rotateSmoothly(rot: Rotation, time: Long, block: () -> Unit = {}) {
        queueRotation {
            val currentYaw = normalizeYaw(mc.thePlayer?.rotationYaw ?: return@queueRotation)
            val currentPitch = normalizePitch(mc.thePlayer?.rotationPitch ?: return@queueRotation)
            val targetYaw = normalizeYaw(rot.yaw)
            val targetPitch = normalizePitch(rot.pitch)
            val tolerance = 1.0f

            if (abs(currentYaw - targetYaw) <= tolerance && abs(currentPitch - targetPitch) <= tolerance) {
                block()
                return@queueRotation
            }

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
                val newPitch = interpolate(currentPitch, targetPitch, easedProgress).toFloat()

                rotate(newYaw, newPitch)
            }
        }
    }

    fun rotateSmoothlyTo(vec: Vec3, time: Long, block: () -> Unit = {}) {
        rotateSmoothly(calcYawPitch(vec), time, block)
    }

    private suspend fun rodSwapAction() {
        if (thePlayer?.isDead == true) return
        val slotBeforeSwap = mc.thePlayer?.inventory?.currentItem ?: return
        val rodIndex = getItemIndexInHotbar("rod") ?: return modMessage("&cNo rod found in hotbar!")
        val keyState = mc.gameSettings.keyBindUseItem.isKeyDown

        if (keyState) holdClick(false)
        swapToSlot(rodIndex)
        delay(80)
        sendRightClickAirPacket()
        mc.thePlayer.swingItem()
        delay(80)
        swapToSlot(slotBeforeSwap)
        holdClick(keyState)
        delay(100)
    }

    private suspend fun leapAction(leapTarget: DungeonPlayer) {
        if (thePlayer?.isDead == true) return
        if (getItemIndexInHotbar("Haunt") != null) return
        if (leapTarget.isDead) return modMessage(leapTarget.name + " is dead R.I.P!")
        val leapIndex = getItemIndexInHotbar("leap") ?: return modMessage("&cNo leap found in hotbar!")

        if (! inLeapMenu) {
            swapToSlot(leapIndex)
            delay(80)
            sendRightClickAirPacket()
            INTERNAL_LEAP_TARGET = leapTarget
            setTimeout(5000) { INTERNAL_LEAP_TARGET = null }

            while (! inLeapMenu && INTERNAL_LEAP_TARGET != null) delay(50)
            delay(400)
        }

        CustomSpiritLeapMenu.updatePlayersArray()
        CustomSpiritLeapMenu.players.find { it?.player?.name == leapTarget.name }?.let { target ->
            sendWindowClickPacket(target.slot, 0, 0)
        }
    }

    private suspend fun changeMaskAction() {
        if (thePlayer?.isDead == true) return
        val currentHelmet = getHelmet()?.SkyblockID
        when (currentHelmet) {
            "BONZO_MASK" -> if (MaskTimers.Masks.SPIRIT_MASK.cooldownTime > 0) return
            "SPIRIT_MASK" -> if (MaskTimers.Masks.BONZO_MASK.cooldownTime > 0) return
        }

        C01PacketChatMessage("/eq").send()
        hideGui(true) { drawTitle("&5[Swapping mask...]", "&bPlease wait") }
        awaiting4EQ = true
        setTimeout(5000) { awaiting4EQ = false }

        while (! inEQMenu && awaiting4EQ) delay(50)
        delay(400)
    }

    private suspend fun getPotionAction(name: String) {
        if (mc.thePlayer?.inventory?.mainInventory?.contains(null) != true) return modMessage("&cYour inventory is full!")

        closeScreen()
        sendChatMessage(config.AutoPotionCommand.removeFormatting().lowercase())
        hideGui(true) { drawTitle("&d[Getting potion...] ", "&bPlease wait") }

        awaitingPotionBag = name
        setTimeout(5000) { awaitingPotionBag = "" }

        while (! inPotionBag && awaitingPotionBag.isNotBlank()) delay(50)
        delay(250)
    }

    suspend fun reaperSwapAction() {
        if (thePlayer?.isDead == true) return

        C01PacketChatMessage("/wd").send()
        hideGui(true) { drawTitle("&8[Swapping armor...] ", "&bPlease wait") }

        while (! inWardrobeMenu) delay(50)
        delay(250)

        val container = mc.thePlayer.openContainer.inventory
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
    fun handleAutoLeap(event: InventoryFullyOpenedEvent) {
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
                if (INTERNAL_LEAP_TARGET == null) return
                CustomSpiritLeapMenu.updatePlayersArray()
                CustomSpiritLeapMenu.players.find { it?.player?.name == INTERNAL_LEAP_TARGET?.name }?.let { target ->
                    modMessage("Leaping To: ${target.player.name}&r (${target.player.clazz})")
                    sendWindowClickPacket(target.slot, 0, 0)
                    INTERNAL_LEAP_TARGET = null
                }
            }

            inEQMenu -> {
                if (! awaiting4EQ) return
                val con = mc.thePlayer?.openContainer?.inventorySlots ?: return resetEQ("&cNo mask found!")

                val mask = con.filter { it.slotNumber in con.size - 36 until con.size }
                    .find { it.stack?.SkyblockID?.containsOneOf("SPIRIT_MASK", "BONZO_MASK") == true }
                    ?: return resetEQ("&cMask found but not the slot?????")

                sendWindowClickPacket(mask.slotNumber, 0, 0)
                resetEQ("Quick swap to ${mask.stack?.displayName}")
            }
        }
    }

    init {
        ThreadUtils.loop(250) {
            if (! LocationUtils.inDungeon) return@loop
            if (thePlayer == null) return@loop
            if (thePlayer?.isDead == true) return@loop

            actionQueue.clear()
            rotationQueue.clear()
        }
    }

    private fun resetEQ(message: String? = null) {
        setTimeout(1000) { hideGui(false) }
        message?.let { modMessage(it) }
        closeScreen()
        SoundUtils.Pling()
        awaiting4EQ = false
    }
}
