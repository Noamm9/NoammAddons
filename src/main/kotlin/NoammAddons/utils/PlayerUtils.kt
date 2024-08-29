package NoammAddons.utils

import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.ChatUtils.modMessage
import NoammAddons.utils.ChatUtils.removeFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import java.util.Timer
import java.util.TimerTask
import kotlin.math.*

object PlayerUtils {
    fun closeScreen() {
        if (mc.currentScreen != null && mc.thePlayer != null) {
            mc.thePlayer.closeScreen()
        }
    }

    fun rightClick() {
        val method = try {
            Minecraft::class.java.getDeclaredMethod("func_147121_ag")
        } catch (e: NoSuchMethodException) {
            Minecraft::class.java.getDeclaredMethod("rightClickMouse")
        }
        method.isAccessible = true
        method.invoke(mc)
    }

    fun leftClick() {
        val method = try {
            Minecraft::class.java.getDeclaredMethod("func_147116_af")
        } catch (e: NoSuchMethodException) {
            Minecraft::class.java.getDeclaredMethod("clickMouse")
        }
        method.isAccessible = true
        method.invoke(mc)
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
        val mc = Minecraft.getMinecraft()
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
        mc.thePlayer.dropOneItem(!Ultimate)
    }


    /**
     * Calculates the yaw and pitch angles required to look at a specific block position.
     *
     * @param blockPos The block position object containing the x, y, and z coordinates.
     * @param playerPos The player position object containing the x, y, and z coordinates. If not provided, the player's eye position will be used.
     *
     * @return A Pair containing the yaw and pitch angles in degrees. If the calculation fails, returns null.
     */
    fun calcYawPitch(blockPos: Vec3, playerPos: Vec3? = getEyePos()): Pair<Float, Float>? {
        val playerPosition = playerPos ?: getEyePos()

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

        return yaw to pitch
    }


    /**
     * Returns the player's eye position.
     *
     * @return Vec3 containing the x, y, and z coordinates of the player's eye position.
     */
    private fun getEyePos(): Vec3 {
        val Player = mc.thePlayer
        return Vec3(Player.posX, Player.posY + Player.getEyeHeight(), Player.posZ)
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

        // Normalize yaw to be within -180 to 180 degrees
        while (targetYaw >= 180) targetYaw -= 360
        while (targetYaw < -180) targetYaw += 360

        val initialYaw = mc.thePlayer.rotationYaw
        val initialPitch = mc.thePlayer.rotationPitch
        val initialTime = System.currentTimeMillis()


        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (cancelCheck) {
                    timer.cancel()
                    return
                }

                val currentTime = System.currentTimeMillis()
                val progress = if (time <= 0) 1.0 else max(min((currentTime - initialTime).toDouble() / time, 1.0), 0.0)

                // Cubic Bezier curve interpolation
                val amount = (1 - progress).pow(3) * 0 +
                        3 * (1 - progress).pow(2) * progress * 1 +
                        3 * (1 - progress) * progress.pow(2) * 1 +
                        progress.pow(3) * 1

                val newYaw = initialYaw + (targetYaw - initialYaw) * amount.toFloat()
                val newPitch = initialPitch + (targetPitch - initialPitch) * amount.toFloat()

                rotate(newYaw, newPitch)

                if (progress >= 1.0) {
                    timer.cancel()
                }
            }
        }, 0, 1000L / 60L)  // Runs every 1/60th of a second
    }

    /**
     * Rotates the player to the given yaw and pitch instantly.
     *
     * @param yaw The yaw value to rotate to.
     * @param pitch The pitch value to rotate to.
     */
    fun rotate(yaw: Float, pitch: Float) {
        mc.thePlayer.rotationYaw = yaw
        mc.thePlayer.rotationPitch = pitch
    }


    /**
     * Swaps the player's inventory to the item in the specified slot.
     * NOTE: The slot index must be in the range of 0-8
     *
     * @param slotIndex The index of the slot to swap to.
     */
    fun swapToSlot(slotIndex: Int) {
        val mcPlayer = mc.thePlayer
        if (mcPlayer == null || slotIndex !in 0..8) return modMessage("&cCannot swap to Slot $slotIndex. Not in hotbar.")

        val mcInventory = mcPlayer.inventory
        mcPlayer.inventory.currentItem = slotIndex

        modMessage("Swapped to ${mcInventory.getStackInSlot(slotIndex)?.displayName ?: "&4&lNOTHING!"}&r in slot &6$slotIndex")
    }
}




