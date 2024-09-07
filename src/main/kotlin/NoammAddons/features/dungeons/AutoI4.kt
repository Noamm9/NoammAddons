package NoammAddons.features.dungeons


import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.CHAT_PREFIX
import NoammAddons.NoammAddons.Companion.FULL_PREFIX
import NoammAddons.utils.GuiUtils
import NoammAddons.utils.PlayerUtils
import NoammAddons.utils.ChatUtils
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.ChatUtils.equalsOneOf
import NoammAddons.utils.BlockUtils.getBlockAt
import NoammAddons.utils.BlockUtils.getBlockId
import NoammAddons.utils.ThreadUtils.setTimeout
import NoammAddons.utils.ItemUtils.getItemId
import NoammAddons.events.ReceivePacketEvent
import NoammAddons.mixins.AccessorKeybinding
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.math.ceil


object AutoI4 {
    private val DevBlocks = listOf(
        Triple(64, 126, 50), Triple(66, 126, 50), Triple(68, 126, 50),
        Triple(64, 128, 50), Triple(66, 128, 50), Triple(68, 128, 50),
        Triple(64, 130, 50), Triple(66, 130, 50), Triple(68, 130, 50)
    )

    private val RightClickKey = mc.gameSettings.keyBindUseItem
    private val doneCoords = mutableSetOf<Triple<Int, Int, Int>>()
    private var wait = false
    private var Alerted = true
    private var TickTimer = -1
    private var shouldPredict = true


    private fun isOnDev(): Boolean {
        val player = mc.thePlayer ?: return false
        return player.posY == 127.0 && player.posX in 62.0..65.0 && player.posZ in 34.0..37.0
    }


    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!config.autoI4) return
        val player = mc.thePlayer ?: return
        if (player.heldItem?.getItemId() != 261) return
        if (!isOnDev()) {
            if (doneCoords.size > 1) PlayerUtils.holdClick(false)
            doneCoords.clear()
            Alerted = false
            return
        }


        val possible = DevBlocks.filterNot { doneCoords.contains(it) }
        if (possible.isEmpty()) return

        val emeraldLocation = possible.find { mc.theWorld.getBlockState(BlockPos(it.first, it.second, it.third)).block.getBlockId() == 133 } ?: return
        doneCoords.add(emeraldLocation)

        val xdiff = when (emeraldLocation.first) {
            68 -> -0.4
            66 -> if (Math.random()*2.0 < 1.0) 1.4 else -0.4
            64 -> 1.4
            else -> 0.5
        }

        val (yaw, pitch) = PlayerUtils.calcYawPitch(
            Vec3(
                emeraldLocation.first + xdiff,
                emeraldLocation.second + 1.1,
                emeraldLocation.third.toDouble()
            )
        ) ?: return


        PlayerUtils.rotateSmoothly(yaw, pitch, 200)
        wait = true
        setTimeout(250) {wait = false}

        setTimeout(310) {if (!RightClickKey.isKeyDown) PlayerUtils.holdClick(true)}

        Thread {
            if (!shouldPredict) return@Thread
            Thread.sleep(350)
            if (mc.theWorld.getBlockAt(BlockPos(emeraldLocation.first, emeraldLocation.second, emeraldLocation.third))?.getBlockId() != 133 || wait) {
                (RightClickKey as AccessorKeybinding).invokeUnpressKey()
                return@Thread
            }

            val remaining = DevBlocks.filterNot { doneCoords.contains(it) }
            if (remaining.isEmpty()) return@Thread

            val nextTarget = remaining.random()
            val xdiffNext = when (nextTarget.first) {
                68 -> -0.4
                66 -> if (Math.random()*2.0 < 1.0) 1.4 else -0.4
                64 -> 1.4
                else -> 0.5
            }

            val (nextYaw, nextPitch) = PlayerUtils.calcYawPitch(
                Vec3(
                    nextTarget.first + xdiffNext,
                    nextTarget.second + 1.1,
                    nextTarget.third.toDouble()
                )
            ) ?: return@Thread

            PlayerUtils.rotateSmoothly(nextYaw, nextPitch, 200)
        }.start()
    }

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        if (event.type.toInt() == 3) return
        if (event.message.unformattedText.removeFormatting().contains("[BOSS] Storm: I should have known that I stood no chance.") ) return
        TickTimer = 0
    }

    @SubscribeEvent
    fun onServerTick(event: ReceivePacketEvent) {
        if (event.packet !is S32PacketConfirmTransaction) return
        if (TickTimer < 0) return
        TickTimer++

        if (TickTimer == 56+144) RodSwapAction.start()

        if (TickTimer == 112+144) LeapAction.start()

        TickTimer = -1
        if (Alerted || !isOnDev()) return

        if (mc.theWorld.loadedEntityList.filter{ it is EntityArmorStand &&
            it.name.removeFormatting().toLowerCase().equalsOneOf("device", "active")}.filter{
                "${ceil(it.posX - 1)}, ${ceil(it.posY + 1)}, ${ceil(it.posZ)}" == "63.0, 127.0, 35.0"
            }.size != 2
            ) return

        Alerted = true
        mc.thePlayer.sendChatMessage("/pc ${CHAT_PREFIX.removeFormatting()} I4 Done!")
        ChatUtils.Alert(FULL_PREFIX, "&a&lI4 Done!")
        if (RightClickKey.isKeyDown) PlayerUtils.holdClick(false)
    }


    private val LeapAction = Thread {
        if (TickTimer < 0) return@Thread

        mc.thePlayer.inventory.mainInventory.forEachIndexed { index, item ->
            if (index > 8 || item == null || !item.displayName.removeFormatting().toLowerCase().contains("leap")) return@forEachIndexed
            if (!isOnDev()) return@forEachIndexed
            PlayerUtils.holdClick(false)

            PlayerUtils.swapToSlot(index)
            Thread.sleep(50)
            PlayerUtils.rightClick()

            while (!GuiUtils.isInGui()) {} // Dynamic cooldown for the thread
            Thread.sleep(150) // Time for items to load

            val container = mc.thePlayer.openContainer
            val mageSlot = container.inventory[14]
            val healerSlot = container.inventory[11]


            when { // dead teammates detection when?
                mageSlot != null -> GuiUtils.clickSlot(14, false, "middle")
                healerSlot != null -> GuiUtils.clickSlot(11, false, "middle")
                container.inventory[12] != null -> GuiUtils.clickSlot(11, false, "middle")
                container.inventory[15] != null -> GuiUtils.clickSlot(15, false, "middle")
            }
        }
    }


    private val RodSwapAction = Thread {
        if (TickTimer < 0) return@Thread
        if (!isOnDev()) return@Thread

        val termSlot = mc.thePlayer.inventory.currentItem
        mc.thePlayer.getInventory().forEachIndexed{ index, item ->
            if (index > 8 || item == null || !item.displayName.removeFormatting().toLowerCase().contains("rod")) return@forEachIndexed
            shouldPredict = false
            PlayerUtils.holdClick(false)
            PlayerUtils.swapToSlot(index)

            Thread.sleep(100)
            PlayerUtils.rightClick()
            Thread.sleep(100)
            PlayerUtils.swapToSlot(termSlot)
            PlayerUtils.holdClick(true)
            shouldPredict = true
        }
    }


    @SubscribeEvent
    fun onDev(event: ClientChatReceivedEvent) {
        if (TickTimer < 0) return
        if (event.type.toInt() == 3) return
        if (!event.message.unformattedText.removeFormatting().matches(Regex("(.+) completed a device! \\(...\\)"))) return
        if (Alerted || !isOnDev()) return

        Alerted = true
        ChatUtils.sendChatMessage("/pc ${CHAT_PREFIX.removeFormatting()} I4 Done!")
        ChatUtils.Alert(FULL_PREFIX, "&a&lI4 Done!")
        if (RightClickKey.isKeyDown) PlayerUtils.holdClick(false)
    }
}
