package noammaddons.utils

import kotlinx.coroutines.*
import net.minecraft.util.Vec3
import noammaddons.features.gui.Menus.impl.CustomWardrobeMenu.inWardrobeMenu
import noammaddons.features.hud.BonzoMask.bonzoCD
import noammaddons.features.hud.SpiritMask.spiritCD
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
import noammaddons.utils.MathUtils.Ease
import noammaddons.utils.MathUtils.Rotation
import noammaddons.utils.MathUtils.calcYawPitch
import noammaddons.utils.MathUtils.interpolate
import noammaddons.utils.MathUtils.normalizePitch
import noammaddons.utils.MathUtils.normalizeYaw
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.PlayerUtils.closeScreen
import noammaddons.utils.PlayerUtils.getHelmet
import noammaddons.utils.PlayerUtils.holdClick
import noammaddons.utils.PlayerUtils.rotate
import noammaddons.utils.PlayerUtils.sendRightClickAirPacket
import noammaddons.utils.PlayerUtils.swapToSlot
import noammaddons.utils.PlayerUtils.toggleSneak
import noammaddons.utils.RenderUtils.drawTitle
import noammaddons.utils.SoundUtils.Pling
import noammaddons.utils.Utils.containsOneOf
import noammaddons.utils.Utils.isNull
import kotlin.math.abs
import kotlin.math.min


@OptIn(DelicateCoroutinesApi::class)
object ActionUtils {
    private var activeJob: Job? = null
    private val actionQueue = ArrayDeque<suspend () -> Unit>()
    private var currentActionName: String? = null
    val isActive get() = activeJob?.isActive == true

    private val RotationQueue = ArrayDeque<suspend () -> Unit>()
    private var rotationJob: Job? = null
    val isRotating get() = rotationJob?.isActive == true

    fun rodSwap() = queueAction("Rod Swap") { rodSwapAction() }

    fun leap(leapTarget: DungeonUtils.DungeonPlayer) = queueAction("Leap") { leapAction(leapTarget) }

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
        if (! isActive) return processQueue()
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

    fun currentAction(): String? = currentActionName


    private fun processRotationQueue() {
        rotationJob = scope.launch {
            while (RotationQueue.isNotEmpty()) {
                try {
                    RotationQueue.removeFirst().invoke()
                }
                catch (e: Exception) {
                    modMessage("Error during Rotation: ${e.message}")
                }
            }
        }
    }

    private fun queueRotation(action: suspend () -> Unit) {
        RotationQueue.add(action)
        if (! isRotating) processRotationQueue()
    }

    fun rotateSmoothly(rot: Rotation, time: Long, block: () -> Unit = {}) {
        queueRotation {
            val currentYaw = normalizeYaw(Player?.rotationYaw ?: return@queueRotation)
            val currentPitch = normalizePitch(Player?.rotationPitch ?: return@queueRotation)
            val targetYaw = normalizeYaw(rot.yaw)
            val targetPitch = normalizePitch(rot.pitch)
            val tolerance = 1.0f
            if (abs(currentYaw - targetYaw) <= tolerance && abs(currentPitch - targetPitch) <= tolerance) return@queueRotation

            val startTime = System.currentTimeMillis()

            scope.launch {
                while (isActive) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val progress = if (time <= 0) 1.0 else min(elapsed.toDouble() / time, 1.0)
                    if (progress >= 1.0) {
                        block()
                        cancel()
                    }

                    val easedProgress = Ease(progress)
                    val newYaw = interpolate(currentYaw, targetYaw, easedProgress).toFloat()
                    val newPitch = interpolate(currentPitch, targetPitch, easedProgress).toFloat()

                    rotate(newYaw, newPitch)
                }
            }
            Thread.sleep(time)
        }
    }

    fun rotateSmoothlyTo(vec: Vec3, time: Long, block: () -> Unit = {}) {
        rotateSmoothly(calcYawPitch(vec), time, block)
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
        if (leapTarget.isDead) return modMessage(leapTarget.name + " is dead R.I.P!")
        val leapIndex = getItemIndexInHotbar("leap") ?: return modMessage("&cNo leap found in hotbar!")

        scope.launch {
            while (isActive) {
                if (currentActionName != "Leap") cancel()
                holdClick(false)
            }
        }

        if (! inLeapMenu) {
            holdClick(false)
            swapToSlot(leapIndex)
            delay(80)
            sendRightClickAirPacket()
            while (! inLeapMenu) delay(1)
        }

        var con = Player.openContainer.inventory
        while (con[con.size - 37].isNull()) {
            delay(1)
            con = Player.openContainer.inventory
        }

        delay(100)

        for (i in 0 until con.size - 37) {
            val item = con[i] ?: continue
            if (item.getItemId() == 160) continue

            leapTeammates.forEach {
                if (it.clazz != leapTarget.clazz) return@forEach
                if (leapTarget.name != item.displayName.removeFormatting()) return@forEach

                sendWindowClickPacket(i, 0, 0)
                modMessage("Leaping ${item.displayName})")
                return
            }
        }
        modMessage("No one alive to leap GG!")
        closeScreen()
    }

    private suspend fun changeMaskAction() {
        val currentHelmet = getHelmet()?.SkyblockID
        when (currentHelmet) {
            "BONZO_MASK" -> if (spiritCD > 0) return
            "SPIRIT_MASK" -> if (bonzoCD > 0) return
        }

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
