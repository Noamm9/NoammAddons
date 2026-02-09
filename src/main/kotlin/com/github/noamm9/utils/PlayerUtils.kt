package com.github.noamm9.utils

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.features.impl.dungeon.LeapMenu
import com.github.noamm9.mixin.IKeyMapping
import com.github.noamm9.ui.utils.Animation.Companion.easeInOutCubic
import com.github.noamm9.utils.ChatUtils.modMessage
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.MathUtils.interpolateYaw
import com.github.noamm9.utils.MathUtils.lerp
import com.github.noamm9.utils.Utils.containsOneOf
import com.github.noamm9.utils.dungeons.DungeonListener.thePlayer
import com.github.noamm9.utils.dungeons.DungeonPlayer
import com.github.noamm9.utils.items.ItemUtils.customData
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.network.PacketUtils.send
import kotlinx.coroutines.delay
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.DROP_ITEM
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.min

object PlayerUtils {
    fun swingArm() = with(mc.player !!) {
        if (! swinging || this.swingTime < 0) {
            swingingArm = InteractionHand.MAIN_HAND
            swingTime = - 1
            swinging = true
        }
    }

    fun toggleSneak(bl: Boolean) {
        mc.options.keyShift.isDown = bl
    }

    fun leftClick() {
        (mc.options.keyAttack as IKeyMapping).clickCount += 1
    }

    fun rightClick() {
        (mc.options.keyUse as IKeyMapping).clickCount += 1
    }

    fun getSelectionBlock(): BlockPos? {
        val hit = mc.hitResult ?: return null
        if (hit.type != HitResult.Type.BLOCK) return null
        return (hit as BlockHitResult).blockPos
    }

    fun useDungeonClassAbility(ult: Boolean) {
        val action = if (ult) DROP_ITEM else DROP_ALL_ITEMS
        ServerboundPlayerActionPacket(action, BlockPos.ZERO, Direction.DOWN).send()
    }

    fun isHoldingEtherwarpItem(itemstack: ItemStack): Boolean {
        if (mc.isSingleplayer && itemstack.`is`(Items.DIAMOND_SHOVEL)) return true
        return itemstack.skyblockId == "ETHERWARP_CONDUIT" || itemstack.customData.getBoolean("ethermerge").orElse(false)
    }

    fun rotate(yaw_: Float, pitch_: Float) = mc.player?.apply {
        var yaw = yRot + MathUtils.normalizeYaw(yaw_ - yRot)
        var pitch = xRot + MathUtils.normalizePitch(pitch_ - xRot)

        val rotations = MathUtils.Rotation(yaw, pitch)
        val lastRotations = MathUtils.Rotation(yRot, xRot)

        val fixedRotations = MathUtils.fixRot(rotations, lastRotations)

        yaw = fixedRotations.yaw
        pitch = fixedRotations.pitch

        pitch = MathUtils.normalizePitch(pitch)

        yRot = yaw
        xRot = pitch

        yHeadRot = yRot
        yBodyRot = yRot

        forceSetRotation(yaw, false, pitch, false)
    }

    fun getHotbarSlot(i: Int): ItemStack? {
        if (! Inventory.isHotbarSlot(i)) return null
        val player = mc.player ?: return null
        return player.inventory.getItem(i)
    }

    fun findHotbarSlot(predicate: (ItemStack) -> Boolean): Int? {
        return (0 .. 8).firstOrNull { idx ->
            val stack = getHotbarSlot(idx) ?: return@firstOrNull false
            if (stack.isEmpty) return@firstOrNull false
            predicate(stack)
        }
    }

    suspend fun rotateSmoothly(rot: MathUtils.Rotation, time: Long, block: suspend () -> Unit = {}) {
        val currentYaw = MathUtils.normalizeYaw(mc.player?.yRot ?: return)
        val currentPitch = MathUtils.normalizePitch(mc.player?.xRot ?: return)
        val targetYaw = MathUtils.normalizeYaw(rot.yaw)
        val targetPitch = MathUtils.normalizePitch(rot.pitch)
        val tolerance = 1f

        if (abs(currentYaw - targetYaw) <= tolerance && abs(currentPitch - targetPitch) <= tolerance) return block()

        val startTime = System.currentTimeMillis()

        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = if (time <= 0) 1.0 else min(elapsed.toDouble() / time, 1.0)
            val easedProgress = easeInOutCubic(progress).toFloat()

            val newYaw = interpolateYaw(currentYaw, targetYaw, easedProgress)
            val newPitch = lerp(currentPitch, targetPitch, easedProgress).toFloat()

            rotate(newYaw, newPitch)

            if (progress >= 1.0) {
                block()
                break
            }

            delay(1)
        }
    }

    suspend fun rotateSmoothly(target: Vec3, time: Long, block: suspend () -> Unit = {}) {
        val rot = MathUtils.calcYawPitch(target)
        rotateSmoothly(rot, time, block)
    }

    suspend fun changeMaskAction() {
        val maskId = mc.player?.inventory?.nonEquipmentItems?.firstNotNullOfOrNull { item ->
            item?.skyblockId?.takeIf { id ->
                id.containsOneOf("SPIRIT_MASK", "BONZO_MASK")
            }
        } ?: return

        quickSwapAction(maskId)
    }

    private var awaiting4EQ = ""
    suspend fun quickSwapAction(itemID: String) {
        if (thePlayer?.isDead == true) return

        ChatUtils.sendMessage("/eq")
        awaiting4EQ = itemID
        ThreadUtils.setTimeout(5000) { awaiting4EQ = "" }

        while (awaiting4EQ.isNotBlank()) delay(50)
    }

    fun swapToSlot(slot: Int) {
        if (! Inventory.isHotbarSlot(slot)) return
        if (mc.player?.inventory?.selectedSlot == slot) return
        modMessage("Swapeed to hotbar Slot $slot")
        mc.player?.inventory?.selectedSlot = slot
    }


    private val inLeapMenu get() = mc.screen?.title?.unformattedText.equals("spirit leap", true)
    private var LEAP_TARGET: DungeonPlayer? = null
    suspend fun leapAction(leapTarget: DungeonPlayer) {
        if (thePlayer?.isDead == true) return
        if (leapTarget.isDead) return modMessage(leapTarget.name + " is dead R.I.P!")
        val leapIndex = findHotbarSlot { it.skyblockId.contains("LEAP") } ?: return modMessage("&cNo leap found in hotbar!")

        if (! inLeapMenu) {
            if (mc.player?.inventory?.selectedSlot != leapIndex) {
                swapToSlot(leapIndex)
                delay(80)
            }
            rightClick()
            LEAP_TARGET = leapTarget
            ThreadUtils.setTimeout(5000) { LEAP_TARGET = null }

            while (LEAP_TARGET != null) delay(50)
        }

        LeapMenu.updateLeapMenu()
        LeapMenu.players.find { it?.player?.name == leapTarget.name }?.let { target ->
            modMessage("Leaping To: &e[${leapTarget.clazz.name[0]}] &a${leapTarget.name}")
            GuiUtils.clickSlot(target.slotIndex, GuiUtils.ButtonType.MIDDLE)
            mc.player?.closeContainer()
        }
    }

    suspend fun rodSwap() {
        val prev = mc.player?.inventory?.selectedSlot ?: return
        val found = findHotbarSlot { it.item == Items.FISHING_ROD } ?: return

        swapToSlot(found)
        delay(100)
        rightClick()
        delay(100)
        swapToSlot(prev)
    }

    init {
        register<ContainerEvent.Open> {
            val title = event.screen.title.unformattedText

            when {
                title.equals("your equipment and stats", true) -> {
                    if (awaiting4EQ.isBlank()) return@register
                    if (! event.screen.title.unformattedText.equals("your equipment and stats", true)) return@register

                    ThreadUtils.scheduledTask(7) {
                        val con = mc.player?.containerMenu?.slots ?: return@scheduledTask

                        val item = con.filter { it.index in con.size - 36 until con.size }.find {
                            it.item?.skyblockId?.contains(awaiting4EQ) == true
                        } ?: return@scheduledTask

                        GuiUtils.clickSlot(item.index, GuiUtils.ButtonType.LEFT)
                        mc.player?.closeContainer()
                        awaiting4EQ = ""
                    }
                }

                title.equals("spirit leap", true) -> {
                    if (LEAP_TARGET == null) return@register

                    ThreadUtils.scheduledTask(2) {
                        LeapMenu.updateLeapMenu()
                        LeapMenu.players.find { it?.player?.name == LEAP_TARGET?.name }?.let { target ->
                            modMessage("Leaping To: &e[${LEAP_TARGET !!.clazz.name[0]}] &a${LEAP_TARGET !!.name}")
                            GuiUtils.clickSlot(target.slotIndex, GuiUtils.ButtonType.LEFT)
                            mc.player?.closeContainer()
                            LEAP_TARGET = null
                        }
                    }
                }
            }

        }
    }
}