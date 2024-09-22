package NoammAddons.features.dungeons

import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.CHAT_PREFIX
import NoammAddons.NoammAddons.Companion.FULL_PREFIX
import NoammAddons.NoammAddons.Companion.MOD_ID
import NoammAddons.events.Chat
import NoammAddons.events.PacketEvent
import NoammAddons.utils.GuiUtils
import NoammAddons.utils.PlayerUtils
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.BlockUtils.getBlockAt
import NoammAddons.utils.BlockUtils.getBlockId
import NoammAddons.utils.ThreadUtils.setTimeout
import NoammAddons.utils.ItemUtils.getItemId
import NoammAddons.mixins.AccessorKeybinding
import NoammAddons.utils.ChatUtils.Alert
import NoammAddons.utils.ChatUtils.equalsOneOf
import NoammAddons.utils.ChatUtils.modMessage
import NoammAddons.utils.ChatUtils.sendChatMessage
import NoammAddons.utils.DungeonUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import kotlin.math.ceil

object AutoI4 {
    private val rightClickKey = mc.gameSettings.keyBindUseItem
    private val doneCoords = mutableSetOf<Triple<Int, Int, Int>>()
    private var wait = false
    private var alerted = true
    private var tickTimer = -1
    private var shouldPredict = true
	private val devBlocks = listOf(
		Triple(64, 126, 50), Triple(66, 126, 50), Triple(68, 126, 50),
		Triple(64, 128, 50), Triple(66, 128, 50), Triple(68, 128, 50),
		Triple(64, 130, 50), Triple(66, 130, 50), Triple(68, 130, 50)
	)

    private fun isOnDev(): Boolean {
        val player = mc.thePlayer ?: return false
        return player.posY == 127.0 && player.posX in 62.0..65.0 && player.posZ in 34.0..37.0
    }

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (!config.autoI4) return
        val player = mc.thePlayer ?: return
        if (player.heldItem?.getItemId() != 261) return
        if (!isOnDev()) {
            synchronized(doneCoords) {
                if (doneCoords.size > 1) PlayerUtils.holdClick(false)
                doneCoords.clear()
            }
            alerted = false
            shouldPredict = true
            return
        }

        val possible = synchronized(doneCoords) {
            devBlocks.filterNot { doneCoords.contains(it) }
        }
        if (possible.isEmpty()) return

        val emeraldLocation = possible.find {
            mc.theWorld.getBlockState(BlockPos(it.first, it.second, it.third)).block.getBlockId() == 133
        } ?: return

        synchronized(doneCoords) {
            doneCoords.add(emeraldLocation)
        }

        val xdiff = when (emeraldLocation.first) {
            68 -> -0.4
            66 -> if (Math.random() * 2.0 < 1.0) 1.4 else -0.4
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
        setTimeout(250) { wait = false }

        setTimeout(310) { if (!rightClickKey.isKeyDown) PlayerUtils.holdClick(true) }

        setTimeout(350) {
            if (!shouldPredict) return@setTimeout

            val blockId = mc.theWorld.getBlockAt(BlockPos(emeraldLocation.first, emeraldLocation.second, emeraldLocation.third))?.getBlockId()
            if (blockId != 133 || wait) {
                (rightClickKey as AccessorKeybinding).invokeUnpressKey()
                return@setTimeout
            }

            val remaining = synchronized(doneCoords) {
                devBlocks.filterNot { doneCoords.contains(it) }
            }
            if (remaining.isEmpty()) return@setTimeout

            val nextTarget = remaining.random()
            val xdiffNext = when (nextTarget.first) {
                68 -> -0.4
                66 -> if (Math.random() * 2.0 < 1.0) 1.4 else -0.4
                64 -> 1.4
                else -> 0.5
            }

            val (nextYaw, nextPitch) = PlayerUtils.calcYawPitch(
                Vec3(
                    nextTarget.first + xdiffNext,
                    nextTarget.second + 1.1,
                    nextTarget.third.toDouble()
                )
            ) ?: return@setTimeout

            PlayerUtils.rotateSmoothly(nextYaw, nextPitch, 200)
        }
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
		val msg = event.component.unformattedText.removeFormatting()
        if (msg == "[BOSS] Storm: I should have known that I stood no chance.") {
            tickTimer = 0
	        return
        }

        if (tickTimer < 0) return
        if (!msg.matches(Regex("(.+) completed a device! \\(...\\)"))) return
        if (alerted || !isOnDev()) return

        alerted = true
        sendChatMessage("/pc ${CHAT_PREFIX.removeFormatting()} I4 Done!")
        Alert(FULL_PREFIX, "&a&lI4 Done!")
        if (rightClickKey.isKeyDown || rightClickKey.isPressed) PlayerUtils.holdClick(false)
    }


    @SubscribeEvent
    @OptIn(DelicateCoroutinesApi::class)
    fun onServerTickReceived(event: PacketEvent.Received) {
        if (event.packet !is S32PacketConfirmTransaction) return
        if (tickTimer == -1) return
        tickTimer++

        // Launch a coroutine to handle suspending logic
        GlobalScope.launch {

            if (tickTimer == 20 + 154) rodSwapAction()

            if (tickTimer == 107 + 154) leapAction()

            // time off message - 104
            // 2 - at start - 40 ticks
	        
            // Phoenix - 80 ticks
            // bonzo - 60 ticks
	        // Spirit - 50 ticks

            if (alerted) return@launch

            val loadedEntities = mc.theWorld.loadedEntityList
                .filter { it is EntityArmorStand && it.name.removeFormatting().equalsOneOf("device", "active") }
                .filter { "${ceil(it.posX - 1)}, ${ceil(it.posY + 1)}, ${ceil(it.posZ)}" == "63.0, 127.0, 35.0" }

            if (loadedEntities.size != 2) return@launch

            alerted = true
            tickTimer = -1

            sendChatMessage("/pc ${CHAT_PREFIX.removeFormatting()} I4 Done!")
            mc.thePlayer.playSound("$MOD_ID:notificationsound", 1f, 1f)

            if (rightClickKey.isKeyDown) PlayerUtils.holdClick(false)
        }
    }



    private suspend fun leapAction() {
        if (!isOnDev()) return
        if (config.DevMode) modMessage("Debug: Starting leap action.")
	    if (GuiUtils.currentChestName.removeFormatting().toLowerCase() == "spirit leap") return

        mc.thePlayer.inventory.mainInventory.forEachIndexed { index, _item ->
            if (index > 8) return@forEachIndexed
            if (_item == null || !_item.displayName.removeFormatting().contains("leap", true)) return@forEachIndexed
            PlayerUtils.holdClick(false)

            PlayerUtils.swapToSlot(index)

            if (config.DevMode) modMessage("Debug: Found leap item in slot $index.")
            PlayerUtils.rightClick()

            // Wait until the leap gui opens aka dynamic delay for ping difference
            while (GuiUtils.currentChestName.toLowerCase() != "spirit leap") {
                delay(25)
            }

            // wait for items to load inside the menu
            delay(150)

            val container = mc.thePlayer.openContainer.inventory
            var clicked = false

            val classPriority = listOf("Mage", "Tank", "Healer", "Archer") // correct me if wrong

            for (priorityClass in classPriority) {
                if (clicked) break

                for (i in 0 until container.size - 1) {
                    val item = container[i] ?: continue
                    if (item.getItemId() == 160) continue

                    DungeonUtils.leapTeammates.forEach { teammate ->
                        if (teammate.clazz.name == priorityClass && teammate.name == item.displayName.removeFormatting() && !teammate.isDead) {
                            GuiUtils.clickSlot(i)
                            clicked = true
                            mc.thePlayer.closeScreen()

                            if (config.DevMode) modMessage("Debug: Clicked on slot: $i for class: ${teammate.clazz.name}")
                            return
                        }
                    }
                }
            }
            if (!clicked) modMessage("No one alive to leap GG!")
        }
    }


    private suspend fun rodSwapAction() {
        if (!isOnDev()) return

        val slotBeforeSwap = mc.thePlayer.inventory.currentItem
        mc.thePlayer.inventory.mainInventory.forEachIndexed { index, item ->
            if (index > 8 || item == null || !item.displayName.removeFormatting().contains("rod", true)) return@forEachIndexed

            shouldPredict = false
            PlayerUtils.holdClick(false)
            PlayerUtils.swapToSlot(index)
            delay(100)

            PlayerUtils.rightClick()
            delay(100)

            PlayerUtils.swapToSlot(slotBeforeSwap)
            PlayerUtils.holdClick(true)
            shouldPredict = true

            return
        }
    }
}
