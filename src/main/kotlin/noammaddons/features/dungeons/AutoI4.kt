package noammaddons.features.dungeons

import noammaddons.noammaddons.Companion.mc
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.CHAT_PREFIX
import noammaddons.noammaddons.Companion.FULL_PREFIX
import noammaddons.mixins.AccessorKeybinding
import noammaddons.sounds.notificationsound
import noammaddons.events.PacketEvent
import noammaddons.events.Chat
import noammaddons.utils.GuiUtils
import noammaddons.utils.PlayerUtils
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.ItemUtils.getItemIndexInHotbar
import noammaddons.utils.ChatUtils
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.equalsOneOf
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.BlockUtils.toVec3
import noammaddons.utils.DungeonUtils.leapTeammates
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.BlockPos
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noammaddons.utils.ItemUtils.isNothing
import java.util.*
import kotlin.math.ceil

object AutoI4 {
    private val rightClickKey = mc.gameSettings.keyBindUseItem
    private val doneCoords = mutableSetOf<BlockPos>()
	private val classPriority = listOf("Mage", "Tank", "Healer", "Archer") // correct me if wrong
    private var wait = false
    private var alerted = true
	private var Leaped = false
    private var tickTimer = -1
    private var shouldPredict = true
	private val devBlocks = listOf(
		BlockPos(64, 126, 50), BlockPos(66, 126, 50), BlockPos(68, 126, 50),
		BlockPos(64, 128, 50), BlockPos(66, 128, 50), BlockPos(68, 128, 50),
		BlockPos(64, 130, 50), BlockPos(66, 130, 50), BlockPos(68, 130, 50)
	)

    private fun isOnDev(): Boolean {
        val player = mc.thePlayer ?: return false
        return player.posY == 127.0 && player.posX in 62.0..65.0 && player.posZ in 34.0..37.0
    }
	
	private fun isInLeapMenu(): Boolean {
		return GuiUtils.currentChestName.removeFormatting()
			.lowercase(Locale.getDefault()) == "spirit leap"
	}
	
	
    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!config.autoI4) return
        if (event.phase != TickEvent.Phase.START) return
        val player = mc.thePlayer ?: return
        if (player.heldItem?.getItemId() != 261) return
        if (!isOnDev()) {
            synchronized(doneCoords) {
                if (doneCoords.size > 1) PlayerUtils.holdClick(false)
                doneCoords.clear()
            }
            alerted = false
            shouldPredict = true
	        Leaped = false
            return
        }

        val possible = synchronized(doneCoords) {
            devBlocks.filterNot { doneCoords.contains(it) }
        }
        if (possible.isEmpty()) return

        val emeraldLocation = possible.find {
            mc.theWorld.getBlockAt(it)?.getBlockId() == 133
        } ?: return

        synchronized(doneCoords) {
            doneCoords.add(emeraldLocation)
        }

        val xdiff = when (emeraldLocation.x) {
            68 -> -0.4
            66 -> if (Math.random() < 0.5) 1.4 else -0.4
            64 -> 1.4
            else -> 0.5
        }

        val (yaw, pitch) = PlayerUtils.calcYawPitch(
	        emeraldLocation.toVec3().add(Vec3(xdiff, 1.1, .0))
		) ?: return

        PlayerUtils.rotateSmoothly(yaw, pitch, 200)
	    
        if (!rightClickKey.isKeyDown) PlayerUtils.holdClick(true)

        setTimeout(350) {
            if (!shouldPredict) return@setTimeout

            val blockId = mc.theWorld.getBlockAt(emeraldLocation)?.getBlockId()
            if (blockId != 133 || wait) return@setTimeout
            

            val remaining = synchronized(doneCoords) {
                devBlocks.filterNot { doneCoords.contains(it) }
            }
	        
            if (remaining.isEmpty()) return@setTimeout

            val nextTarget = remaining.random()
            val xdiffNext = when (nextTarget.x) {
                68 -> -0.4
                66 -> if (Math.random() * 2.0 < 1.0) 1.4 else -0.4
                64 -> 1.4
                else -> 0.5
            }

            val (nextYaw, nextPitch) = PlayerUtils.calcYawPitch(
	            nextTarget.toVec3().add(Vec3(xdiffNext, 1.1, .0))
			) ?: return@setTimeout

            PlayerUtils.rotateSmoothly(nextYaw, nextPitch, 200)
        }
    }

    @SubscribeEvent
    @OptIn(DelicateCoroutinesApi::class)
    fun onChat(event: Chat) {
		if (!config.autoI4) return
		val msg = event.component.unformattedText.removeFormatting()
        when {
	        msg == "[BOSS] Storm: I should have known that I stood no chance." -> {
		        tickTimer = 0
		        return
	        }
	        msg.matches(Regex("(.+) completed a device! \\(...\\)")) -> {
                if (tickTimer < 0) return
                if (alerted || !isOnDev()) return
		        
                alerted = true
		        GlobalScope.launch { leapAction() }
                ChatUtils.sendChatMessage("/pc ${CHAT_PREFIX.removeFormatting()} I4 Done!")
                ChatUtils.Alert(FULL_PREFIX, "&a&lI4 Done!")
                if (rightClickKey.isKeyDown || rightClickKey.isPressed) PlayerUtils.holdClick(false)
			}
		}
    }


    @SubscribeEvent
    @OptIn(DelicateCoroutinesApi::class)
    fun onServerTickReceived(event: PacketEvent.Received) {
	    if (!config.autoI4) return
        if (event.packet !is S32PacketConfirmTransaction) return
        if (tickTimer == -1) return
        tickTimer++
	    
        GlobalScope.launch {

            if (tickTimer == 25 + 144) rodSwapAction()

            if (tickTimer == 107 + 144) leapAction()

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
	        leapAction()
            tickTimer = -1

            ChatUtils.sendChatMessage("/pc ${CHAT_PREFIX.removeFormatting()} I4 Done!")
	        notificationsound.play()

            if (rightClickKey.isKeyDown) PlayerUtils.holdClick(false)
        }
    }



    private suspend fun leapAction() {
	    if (!config.autoI4) return
	    if (Leaped) return
        if (!isOnDev()) return
	    if (isInLeapMenu()) return
	    val leapIndex = getItemIndexInHotbar("leap") ?: return
	    ChatUtils.debugMessage("Debug: Starting leap action.")
	    
	    PlayerUtils.holdClick(false)
	    PlayerUtils.swapToSlot(leapIndex)
	    Leaped = true
	    delay(80)
	    PlayerUtils.sendRightClickAirPacket()

	    // Wait until the leap gui opens aka dynamic delay for ping difference
	    while (!isInLeapMenu()) {
		    delay(1)
	    }
	    
	    var container = mc.thePlayer.openContainer.inventory
	    while (container?.get(16).isNothing()) {
		    delay(1)
		    container = mc.thePlayer.openContainer.inventory
	    }
	    
	    var clicked = false
	    
	    for (priorityClass in classPriority) {
			if (clicked) break

		    for (i in 0 until container.size - 1) {
				val item = container[i] ?: continue
			    if (item.getItemId() == 160) continue

			    leapTeammates.forEach {
					if (it.clazz.name == priorityClass && it.name == item.displayName.removeFormatting() && !it.isDead) {
						GuiUtils.clickSlot(i)
						clicked = true
						PlayerUtils.closeScreen()

						ChatUtils.debugMessage("Debug: Clicked on slot: $i for class: ${it.clazz.name}")
						return
					}
				}
			}
		}
	    if (!clicked) ChatUtils.modMessage("No one alive to leap GG!")
    }


    private suspend fun rodSwapAction() {
	    if (!config.autoI4) return
		if (!isOnDev()) return
        val slotBeforeSwap = mc.thePlayer.inventory.currentItem
		val rodIndex = getItemIndexInHotbar("rod") ?: return

	    shouldPredict = false
	    PlayerUtils.holdClick(false)
	    PlayerUtils.swapToSlot(rodIndex)
	    delay(80)
	    PlayerUtils.sendRightClickAirPacket()
	    delay(80)
	    PlayerUtils.swapToSlot(slotBeforeSwap)
	    PlayerUtils.holdClick(true)
	    delay(100)
	    shouldPredict = true
    }
}
