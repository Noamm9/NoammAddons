package com.github.noamm9.utils

import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.impl.ContainerFullyOpenedEvent
import com.github.noamm9.features.Shortcuts
import com.github.noamm9.features.impl.dungeon.LeapMenu
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.mixin.IKeyMapping
import com.github.noamm9.mixin.ILocalPlayer
import com.github.noamm9.ui.utils.Animation.Companion.easeInOutCubic
import com.github.noamm9.utils.ActionUtils.waitTicks
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ChatUtils.modMessage
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.MathUtils.interpolateYaw
import com.github.noamm9.utils.MathUtils.lerp
import com.github.noamm9.utils.Utils.send
import com.github.noamm9.utils.dungeons.DungeonListener.thePlayer
import com.github.noamm9.utils.dungeons.DungeonPlayer
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import kotlinx.coroutines.delay
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.DROP_ITEM
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.min

object PlayerUtils: ISelfInit, Shortcuts {
    val LocalPlayer.serverYaw get() = (this as ILocalPlayer).serverYaw
    val LocalPlayer.serverPitch get() = (this as ILocalPlayer).serverPitch

    fun swingArm() {
        if (! player.swinging || player.swingTime < 0) {
            player.swingingArm = InteractionHand.MAIN_HAND
            player.swingTime = - 1
            player.swinging = true
        }
    }

    fun toggleSneak(bl: Boolean) {
        mc.options.keyShift.isDown = bl
    }

    fun leftClick() {
        val key = mc.options.keyAttack
        key.isDown = true
        (key as IKeyMapping).clickCount += 1
        key.isDown = false
    }

    fun rightClick() {
        val key = mc.options.keyUse
        key.isDown = true
        (key as IKeyMapping).clickCount += 1
        key.isDown = false
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

    fun rotate(yaw_: Float, pitch_: Float) {
        var yaw = player.yRot + MathUtils.normalizeYaw(yaw_ - player.yRot)
        var pitch = player.xRot + MathUtils.normalizePitch(pitch_ - player.xRot)

        val rotations = MathUtils.Rotation(yaw, pitch)
        val lastRotations = MathUtils.Rotation(player.yRot, player.xRot)

        val fixedRotations = MathUtils.fixRot(rotations, lastRotations)

        yaw = fixedRotations.yaw
        pitch = fixedRotations.pitch

        pitch = MathUtils.normalizePitch(pitch)

        player.yRot = yaw
        player.xRot = pitch

        player.yHeadRot = player.yRot
        player.yBodyRot = player.yRot

        player.forceSetRotation(yaw, false, pitch, false)
    }

    fun getHotbarSlot(i: Int): ItemStack? {
        if (! Inventory.isHotbarSlot(i)) return null
        return player.inventory.getItem(i)
    }

    fun findHotbarSlot(predicate: (ItemStack) -> Boolean): Int? {
        return (0 .. 8).firstOrNull { idx ->
            val stack = getHotbarSlot(idx) ?: return@firstOrNull false
            if (stack.isEmpty) return@firstOrNull false
            predicate(stack)
        }
    }

    fun getArmor() = listOf(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD).map(player::getItemBySlot)

    suspend fun rotateSmoothly(rot: MathUtils.Rotation, time: Long, block: suspend () -> Unit = {}) {
        val currentYaw = MathUtils.normalizeYaw(player.yRot)
        val currentPitch = MathUtils.normalizePitch(player.xRot)
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


    private var awaiting4EQ = emptyList<String>()
    private val inLeapMenu get() = mc.screen?.title?.unformattedText.equals("spirit leap", true)
    private var awaitingLeap: DungeonPlayer? = null

    suspend fun changeMaskAction() = quickSwapAction("SPIRIT_MASK", "BONZO_MASK")


    suspend fun quickSwapAction(vararg itemIDs: String) {
        if (thePlayer?.isDead == true) return

        ChatUtils.sendMessage("/stats")
        awaiting4EQ = itemIDs.toList()
        ThreadUtils.setTimeout(5000) { awaiting4EQ = emptyList() }

        while (awaiting4EQ.isNotEmpty()) delay(50)
    }

    fun swapToSlot(slot: Int) {
        if (! Inventory.isHotbarSlot(slot)) return
        if (player.inventory.selectedSlot == slot) return
        modMessage("swapped to hotbar Slot $slot (${player.inventory.getSlot(slot)?.get()?.hoverName?.formattedText}&r)")
        player.inventory.selectedSlot = slot
    }

    suspend fun leapAction(leapTarget: DungeonPlayer) {
        if (thePlayer?.isDead == true) return
        if (leapTarget.isDead) return modMessage(leapTarget.name + " is dead R.I.P!")
        val leapIndex = findHotbarSlot { it.skyblockId.contains("LEAP") } ?: return modMessage("&cNo leap found in hotbar!")

        if (! inLeapMenu) {
            if (player.inventory.selectedSlot != leapIndex) {
                swapToSlot(leapIndex)
                waitTicks(2)
            }
            rightClick()
            awaitingLeap = leapTarget
            ThreadUtils.setTimeout(5000) { awaitingLeap = null }

            while (awaitingLeap != null) delay(50)
        }

        LeapMenu.updateLeapMenu()
        LeapMenu.players.find { it?.player?.name == leapTarget.name }?.let { target ->
            modMessage("Leaping To: &e[${leapTarget.clazz.name[0]}] &a${leapTarget.name}")
            GuiUtils.clickSlot(target.slotIndex, GuiUtils.ButtonType.MIDDLE)
            player.closeContainer()
        }
    }

    suspend fun rodSwap() {
        val found = findHotbarSlot { it.item == Items.FISHING_ROD } ?: return modMessage("&cNo Fishing Rod found in hotbar!")
        val prev = player.inventory.selectedSlot

        swapToSlot(found)
        waitTicks(2, ::rightClick)
        waitTicks(2) { swapToSlot(prev) }
        delay(100)
    }

    override fun init() {
        register<ContainerFullyOpenedEvent> {
            when (event.title.unformattedText.lowercase().trim()) {
                "stats & equipment" -> {
                    if (awaiting4EQ.isEmpty()) return@register

                    ThreadUtils.scheduledTask(7) {
                        val con = player.containerMenu.slots
                        val item = con.filter { it.index in con.size - 36 until con.size }.find { slot ->
                            awaiting4EQ.any(slot.item.skyblockId::contains)
                        } ?: run {
                            player.closeContainer()
                            return@scheduledTask modMessage("&cCould not find any of the items. ${awaiting4EQ.joinToString(", ")}")
                        }

                        GuiUtils.clickSlot(item.index, GuiUtils.ButtonType.LEFT)
                        awaiting4EQ = emptyList()
                        player.closeContainer()
                    }
                }

                "spirit leap" -> {
                    if (awaitingLeap == null) return@register

                    ThreadUtils.scheduledTask(2) {
                        val leapTarget = awaitingLeap ?: return@scheduledTask

                        LeapMenu.updateLeapMenu()
                        LeapMenu.players.find { it?.player?.name == leapTarget.name }?.let { target ->
                            modMessage("Leaping To: &e[${leapTarget.clazz.name[0]}] &a${leapTarget.name}")
                            GuiUtils.clickSlot(target.slotIndex, GuiUtils.ButtonType.LEFT)
                        }
                        player.closeContainer()
                        awaitingLeap = null
                    }
                }
            }
        }
    }
}