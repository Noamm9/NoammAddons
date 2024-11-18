package noammaddons.utils

import kotlinx.coroutines.*
import net.minecraft.client.settings.KeyBinding
import net.minecraft.client.settings.KeyBinding.setKeyBindState
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.Vec3
import noammaddons.features.misc.PlayerScale.getPlayerScaleFactor
import noammaddons.noammaddons.Companion.mc
import noammaddons.noammaddons.Companion.scope
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.MathUtils.Ease
import noammaddons.utils.MathUtils.Rotation
import noammaddons.utils.MathUtils.normalizePitch
import noammaddons.utils.MathUtils.normalizeYaw
import noammaddons.utils.ReflectionUtils.invoke
import noammaddons.utils.RenderHelper.getRenderVec
import noammaddons.utils.RenderHelper.interpolate
import noammaddons.utils.Utils.isNull
import kotlin.math.*

object PlayerUtils {
    private var rotationJob: Job? = null
    val Player get() = mc.thePlayer
    val isRotating get() = rotationJob?.isActive ?: false

    fun getPlayerHeight(ent: Entity, add: Number = 0): Float {
        return (1.8f + add.toFloat()) * getPlayerScaleFactor(ent)
    }

    fun closeScreen() {
        if (mc.currentScreen != null && ! Player.isNull()) {
            Player !!.closeScreen()
        }
    }

    fun getArmor(): Array<out ItemStack>? = Player?.inventory?.armorInventory

    fun getHelmet(): ItemStack? = getArmor()?.get(3)
    fun getChestplate(): ItemStack? = getArmor()?.get(2)
    fun getLeggings(): ItemStack? = getArmor()?.get(1)
    fun getBoots(): ItemStack? = getArmor()?.get(0)

    /**
     * Toggles the sneak state of the player.
     *
     * @param isSneaking A boolean indicating whether to enable or disable sneaking.
     * If true, sneaking will be enabled. If false, sneaking will be disabled.
     */
    fun toggleSneak(isSneaking: Boolean) {
        setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, isSneaking)
    }

    fun rightClick() {
        if (! invoke(mc, "func_147121_ag")) {
            invoke(mc, "rightClickMouse")
        }
    }

    fun leftClick() {
        if (! invoke(mc, "func_147116_af")) {
            invoke(mc, "clickMouse")
        }
    }

    fun middleClick() {
        if (! invoke(mc, "func_147112_ai")) {
            invoke(mc, "middleClickMouse")
        }
    }

    fun sendRightClickAirPacket() {
        mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(Player?.heldItem))
    }

    /**
     * Holds a mouse button
     *
     * @param hold A boolean indicating whether to Hold or Release.
     * If true, Hold. If false, Release.
     *
     * @param type The type of mouse click. Can be "LEFT", "RIGHT", or "MIDDLE".
     * Defaults to "RIGHT".
     */
    fun holdClick(hold: Boolean, type: String = "RIGHT") {
        val cleanedType = type.removeFormatting().lowercase()
        when (cleanedType) {
            "right" -> {
                val rightClickKey = mc.gameSettings.keyBindUseItem
                setKeyBindState(rightClickKey.keyCode, hold)
            }

            "left" -> {
                val leftClickKey = mc.gameSettings.keyBindAttack
                setKeyBindState(leftClickKey.keyCode, hold)
            }

            "middle" -> {
                val middleClickKey: KeyBinding = mc.gameSettings.keyBindPickBlock
                setKeyBindState(middleClickKey.keyCode, hold)
            }
        }
    }

    /**
     * @param [Ultimate] A boolean indicating whether to use the ultimate or the regular ability.
     * If true, the ultimate ability will be used. If false, the regular class ability will be used.
     * The default value is false, meaning the regular ability will be used.
     */
    fun useDungeonClassAbility(Ultimate: Boolean = false) {
        Player?.dropOneItem(! Ultimate) ?: return
    }

    fun getEyePos(): Vec3 = Player.run { getRenderVec().add(Vec3(0.0, getEyeHeight().toDouble(), 0.0)) }

    fun rotate(yaw: Float, pitch: Float) {
        Player?.run {
            rotationYaw = yaw
            rotationPitch = pitch
        }
    }

    /**
     * Calculates the yaw and pitch angles required to look at a specific block position.
     *
     * @param blockPos The block position object containing the x, y, and z coordinates.
     * @param playerPos The player position object containing the x, y, and z coordinates. If not provided, the player's eye position will be used.
     *
     * @return A Pair containing the yaw and pitch angles in degrees. If the calculation fails, returns null.
     */
    fun calcYawPitch(blockPos: Vec3, playerPos: Vec3 = getEyePos()): Rotation {
        val dx = blockPos.xCoord - playerPos.xCoord
        val dy = blockPos.yCoord - playerPos.yCoord
        val dz = blockPos.zCoord - playerPos.zCoord

        val yaw = if (dx != 0.0) (- (if (dx < 0) 1.5 * PI else 0.5 * PI - atan(dz / dx)) * 180 / PI).toFloat()
        else if (dz < 0) 180f
        else 0f

        val xzDistance = sqrt(dx.pow(2) + dz.pow(2))
        val pitch = (- (atan(dy / xzDistance) * 180 / PI)).toFloat()

        return Rotation(yaw, pitch)
    }


    /**
     * Rotates the player's view smoothly over a specified time period.
     *
     * @param yaw The new yaw (horizontal rotation) value for the player's view.
     * @param pitch The new pitch (vertical rotation) value for the player's view.
     * @param time The duration in milliseconds over which the rotation should occur.
     * @param cancelCheck Optional parameter to determine if rotation should be canceled.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun rotateSmoothly(yaw: Float, pitch: Float, time: Long, cancelCheck: () -> Boolean = { false }) {
        rotationJob?.cancel()

        val initialYaw = normalizeYaw(Player.rotationYaw)
        val initialPitch = normalizePitch(Player.rotationPitch)
        val targetYaw = normalizeYaw(yaw)
        val targetPitch = normalizePitch(pitch)

        rotationJob = scope.launch {
            val startTime = System.currentTimeMillis()

            while (isActive) {
                if (cancelCheck()) break

                val elapsed = System.currentTimeMillis() - startTime
                val progress = if (time <= 0) 1.0 else min(elapsed.toDouble() / time, 1.0)
                if (progress >= 1.0) break

                val easedProgress = Ease(progress)
                val newYaw = interpolate(initialYaw, targetYaw, easedProgress).toFloat()
                val newPitch = interpolate(initialPitch, targetPitch, easedProgress).toFloat()

                rotate(newYaw, newPitch)
            }
        }
    }

    fun rotateSmoothlyTo(vec: Vec3, time: Long, cancelCheck: () -> Boolean = { false }) = calcYawPitch(vec).run { rotateSmoothly(yaw, pitch, time) { cancelCheck() } }

    /**
     * Swaps the player's inventory to the item in the specified slot.
     * NOTE: The slot index must be in the range of 0-8
     *
     * @param slotIndex The index of the slot to swap to.
     */
    fun swapToSlot(slotIndex: Int) {
        if (Player.isNull() || slotIndex !in 0 .. 8) return modMessage(
            "&cCannot swap to Slot $slotIndex. Not in hotbar."
        )

        val mcInventory = Player !!.inventory
        mcInventory.currentItem = slotIndex

        modMessage("Swapped to ${mcInventory.getStackInSlot(slotIndex)?.displayName ?: "&4&lNOTHING!"}&r in slot &6$slotIndex")
    }

    fun isHoldingWitherImpact(): Boolean {
        val heldItem = Player?.heldItem ?: return false

        val nbt = heldItem.tagCompound ?: return false
        val extraAttributes = nbt.getCompoundTag("ExtraAttributes") ?: return false
        val abilityScroll = extraAttributes.getTagList("ability_scroll", 8).toString()

        return abilityScroll.run {
            contains("SHADOW_WARP_SCROLL") && contains("IMPLOSION_SCROLL") && contains("WITHER_SHIELD_SCROLL")
        }
    }

}