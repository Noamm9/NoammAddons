package noammaddons.utils

import net.minecraft.client.settings.KeyBinding
import net.minecraft.client.settings.KeyBinding.setKeyBindState
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.Vec3
import noammaddons.features.misc.PlayerScale.getPlayerScaleFactor
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.ReflectionUtils.invoke
import noammaddons.utils.RenderHelper.getRenderVec
import noammaddons.utils.Utils.isNull
import noammaddons.utils.Utils.send

object PlayerUtils {
    val Player get() = mc.thePlayer

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
    fun toggleSneak(isSneaking: Boolean = mc.gameSettings.keyBindSneak.isKeyDown) {
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
        C08PacketPlayerBlockPlacement(Player?.heldItem).send()
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
        Player.apply {
            rotationYaw = yaw
            rotationPitch = pitch
        }
    }


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

    fun isHoldingEtherwarpItem(): Boolean {
        val held = Player?.heldItem ?: return false
        val sbId = held.SkyblockID

        if (sbId.contains("ASPECT_OF_THE_END") || sbId.contains("ASPECT_OF_THE_VOID")) return true

        return held.getSubCompound("ExtraAttributes", false)?.getString("ethermerge") == "1"
    }
}