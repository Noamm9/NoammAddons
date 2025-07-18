package noammaddons.utils

import net.minecraft.client.settings.KeyBinding.*
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.Vec3
import noammaddons.NoammAddons.Companion.mc
import noammaddons.features.impl.misc.PlayerModel.getPlayerScaleFactor
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ItemUtils.extraAttributes
import noammaddons.utils.ItemUtils.skyblockID
import noammaddons.utils.MathUtils.Rotation
import noammaddons.utils.MathUtils.add
import noammaddons.utils.ReflectionUtils.invoke
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.send

object PlayerUtils {
    fun getPlayerHeight(ent: Entity, add: Number = 0): Float {
        return (1.8f + add.toFloat()) * getPlayerScaleFactor(ent)
    }

    fun closeScreen() {
        mc.addScheduledTask {
            mc.thePlayer?.closeScreen()
        }
    }

    fun swinghand() {
        (mc.thePlayer as EntityLivingBase).apply {
            swingProgressInt = - 1
            isSwingInProgress = true
        }
    }

    fun getRotation() = Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)

    fun getArmor(): Array<ItemStack?> = mc.thePlayer?.inventory?.armorInventory ?: arrayOf(null, null, null, null)

    fun getHelmet(): ItemStack? = getArmor()[3]
    fun getChestplate(): ItemStack? = getArmor()[2]
    fun getLeggings(): ItemStack? = getArmor()[1]
    fun getBoots(): ItemStack? = getArmor()[0]


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
        C08PacketPlayerBlockPlacement(mc.thePlayer?.heldItem).send()
    }

    fun holdClick(hold: Boolean, type: String = "RIGHT") {
        when (type.uppercase()) {
            "RIGHT" -> {
                val rightClickKey = mc.gameSettings.keyBindUseItem
                setKeyBindState(rightClickKey.keyCode, hold)
            }

            "LEFT" -> {
                val leftClickKey = mc.gameSettings.keyBindAttack
                setKeyBindState(leftClickKey.keyCode, hold)
            }

            "MIDDLE" -> {
                val middleClickKey = mc.gameSettings.keyBindPickBlock
                setKeyBindState(middleClickKey.keyCode, hold)
            }

            else -> println("Invalid click type: $type")
        }

    }

    /**
     * @param [Ultimate] A boolean indicating whether to use the ultimate or the regular ability.
     * If true, the ultimate ability will be used. If false, the regular class ability will be used.
     * The default value is false, meaning the regular ability will be used.
     */
    fun useDungeonClassAbility(Ultimate: Boolean = false) {
        mc.thePlayer?.dropOneItem(! Ultimate) ?: return
    }

    fun getEyePos(): Vec3 = mc.thePlayer.run { renderVec.add(y = getEyeHeight()) }

    fun rotate(yaw: Float, pitch: Float) = mc.thePlayer.apply {
        rotationYaw = yaw
        rotationPitch = pitch
    }


    fun swapToSlot(slotIndex: Int) {
        if (mc.thePlayer == null || slotIndex !in 0 .. 8) return modMessage(
            "&cCannot swap to Slot $slotIndex. Not in hotbar."
        )

        val mcInventory = mc.thePlayer !!.inventory
        mcInventory.currentItem = slotIndex

        modMessage("Swapped to ${mcInventory.getStackInSlot(slotIndex)?.displayName ?: "&4&lNOTHING!"}&r in slot &6$slotIndex")
    }

    fun isHoldingWitherImpact(itemstack: ItemStack? = mc.thePlayer?.heldItem): Boolean {
        return itemstack.extraAttributes?.getTagList("ability_scroll", 8).toString().run {
            contains("SHADOW_WARP_SCROLL") && contains("IMPLOSION_SCROLL") && contains("WITHER_SHIELD_SCROLL")
        }
    }

    fun isHoldingTpItem(itemstack: ItemStack? = mc.thePlayer?.heldItem): Boolean {
        if (mc.isSingleplayer && itemstack?.item == Items.diamond_shovel) return true
        val nbt = itemstack?.getSubCompound("ExtraAttributes", false) ?: return false
        val sbId = itemstack.skyblockID ?: return false

        if (sbId.equalsOneOf("ASPECT_OF_THE_END", "ASPECT_OF_THE_VOID")) return true
        if (nbt.getByte("ethermerge") == 1.toByte()) return true
        return nbt.getTagList("ability_scroll", 8).toString().run {
            contains("SHADOW_WARP_SCROLL") && contains("IMPLOSION_SCROLL") && contains("WITHER_SHIELD_SCROLL")
        }
    }

    fun isHoldingEtherwarpItem(itemstack: ItemStack? = ServerPlayer.player.getHeldItem()): Boolean {
        itemstack ?: return false
        if (mc.isSingleplayer && itemstack.item == Items.diamond_shovel) return true
        val sbId = itemstack.skyblockID ?: return false
        return sbId == "ETHERWARP_CONDUIT" || itemstack.getSubCompound("ExtraAttributes", false).getBoolean("ethermerge")
    }
}