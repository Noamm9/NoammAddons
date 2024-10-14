package noammaddons.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.MathUtils.Rotation
import noammaddons.utils.ThreadUtils.setTimeout
import net.minecraft.client.settings.KeyBinding
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.Vec3
import noammaddons.noammaddons.Companion.config
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.ReflectionUtils.invoke
import noammaddons.utils.Utils.isNull
import java.lang.reflect.Method
import java.util.*
import kotlin.math.*

object PlayerUtils {
	val Player: EntityPlayerSP? get() = mc.thePlayer
	
	fun getPlayerHeight(add: Number = 0): Double {
		return if (config.PlayerScale && config.PlayerScaleOnEveryone) (1.8 + add.toDouble()) * config.PlayerScaleValue
		else 1.8 + add.toDouble()
	}
	
	
    fun closeScreen() {
        if (mc.currentScreen != null && !Player.isNull()) {
	        Player!!.closeScreen()
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
        val sneakKey: KeyBinding = mc.gameSettings.keyBindSneak

        KeyBinding.setKeyBindState(sneakKey.keyCode, isSneaking)
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
        mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(Player!!.heldItem))
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
                val rightClickKey: KeyBinding = mc.gameSettings.keyBindUseItem
                KeyBinding.setKeyBindState(rightClickKey.keyCode, hold)
            }
            "left" -> {
                val leftClickKey: KeyBinding = mc.gameSettings.keyBindAttack
                KeyBinding.setKeyBindState(leftClickKey.keyCode, hold)
            }
            "middle" -> {
                val middleClickKey: KeyBinding = mc.gameSettings.keyBindPickBlock
                KeyBinding.setKeyBindState(middleClickKey.keyCode, hold)
            }
        }
    }


    /**
     * @param [Ultimate] A boolean indicating whether to use the ultimate or the regular ability.
     * If true, the ultimate ability will be used. If false, the regular class ability will be used.
     * The default value is false, meaning the regular ability will be used.
     */
    fun useDungeonClassAbility(Ultimate: Boolean = false) {
	    Player?.dropOneItem(!Ultimate) ?: return
    }


    private fun getEyePos(): Vec3? = Player?.let { Vec3(it.posX, it.posY + it.getEyeHeight(), it.posZ) }
    
    private fun normalizeYaw(yaw: Float): Float {
        var result = yaw
        while (result >= 180) result -= 360
        while (result < -180) result += 360
        return result
    }
    private fun rotate(yaw: Float, pitch: Float) {
		Player?.let {
            it.rotationYaw = yaw
            it.rotationPitch = pitch
		}
    }
	private fun Ease(t: Double): Double = sin((t * Math.PI) / 2)

    /**
     * Calculates the yaw and pitch angles required to look at a specific block position.
     *
     * @param blockPos The block position object containing the x, y, and z coordinates.
     * @param playerPos The player position object containing the x, y, and z coordinates. If not provided, the player's eye position will be used.
     *
     * @return A Pair containing the yaw and pitch angles in degrees. If the calculation fails, returns null.
     */
    fun calcYawPitch(blockPos: Vec3, playerPos: Vec3? = getEyePos()): Rotation? {
        val playerPosition = playerPos ?: getEyePos() ?: return null

        val dx = blockPos.xCoord - playerPosition.xCoord
        val dy = blockPos.yCoord - playerPosition.yCoord
        val dz = blockPos.zCoord - playerPosition.zCoord

        val pitch: Float

        val yaw: Float = if (dx != 0.0) {
            val baseYaw = if (dx < 0) 1.5 * Math.PI else 0.5 * Math.PI
            (-((baseYaw - atan(dz / dx)) * 180 / Math.PI)).toFloat()
        }
        else if (dz < 0) 180f
        else 0f


        val xzDistance = sqrt(dx.pow(2) + dz.pow(2))
        pitch = (-(atan(dy / xzDistance) * 180 / Math.PI)).toFloat()

        if (pitch < -90 || pitch > 90 || yaw.isNaN() || pitch.isNaN()) return null

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
    fun rotateSmoothly(yaw: Float, pitch: Float, time: Long, cancelCheck: Boolean = false) {
        var targetYaw = yaw
        var targetPitch = pitch

        while (targetYaw >= 180) targetYaw -= 360
        while (targetYaw < -180) targetYaw += 360

        if (targetYaw !in -180.0..180.0) {
            return modMessage("&cInvalid yaw value")
        }

        targetPitch = targetPitch.coerceIn(-90f, 90f)

        if (targetPitch !in -90.0..90.0) {
            return modMessage("&cInvalid pitch value")
        }

        val initialYaw = Player?.rotationYaw ?: return
        val initialPitch = Player?.rotationPitch ?: return
        val initialTime = System.currentTimeMillis()
        val deltaYaw = normalizeYaw(targetYaw - initialYaw)
	    
        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (cancelCheck) {
                    timer.cancel()
                    return
                }

                val currentTime = System.currentTimeMillis()
                val progress = if (time <= 0) 1.0 else max(min((currentTime - initialTime).toDouble() / time, 1.0), 0.0)
                val easedProgress = Ease(progress)
                val newYaw = initialYaw + deltaYaw * easedProgress.toFloat()
                val newPitch = initialPitch + (targetPitch - initialPitch) * easedProgress.toFloat()

                rotate(newYaw, newPitch)

                if (progress >= 1.0) {
                    timer.cancel()
                }
            }
        }, 0, 1L)
    }


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

        val mcInventory = Player!!.inventory
	    mcInventory.currentItem = slotIndex

        modMessage("Swapped to ${mcInventory.getStackInSlot(slotIndex)?.displayName ?: "&4&lNOTHING!"}&r in slot &6$slotIndex")
    }
	
	fun isHoldingWitherImpact(): Boolean {
		val heldItem = Player?.heldItem ?: return false
		
		val nbt = heldItem.tagCompound ?: return false
		val extraAttributes = nbt.getCompoundTag("ExtraAttributes") ?: return false
		val abilityScroll = extraAttributes.getTagList("ability_scroll", 8).toString()
		
		return abilityScroll.contains("SHADOW_WARP_SCROLL") &&
		       abilityScroll.contains("IMPLOSION_SCROLL") &&
		       abilityScroll.contains("WITHER_SHIELD_SCROLL")
	}

}