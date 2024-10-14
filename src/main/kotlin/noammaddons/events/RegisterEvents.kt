package noammaddons.events

import noammaddons.noammaddons.Companion.mc
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noammaddons.utils.Utils.isNull


object RegisterEvents {
	private fun PostEvent(event: Event) = MinecraftForge.EVENT_BUS.post(event)
	
	@SubscribeEvent
	fun onTick(event: ClientTickEvent) {
		if (event.phase != TickEvent.Phase.START) return
		if (mc.theWorld.isNull() || mc.thePlayer.isNull()) return
 		PostEvent(Tick())
	}
	
	@SubscribeEvent
	@Suppress("UNUSED_PARAMETER")
	fun onRenderOverlay(event: RenderGameOverlayEvent.Text) {
		if (mc.renderManager?.fontRenderer == null) return
		
		GlStateManager.pushMatrix()
		GlStateManager.translate(0f, 0f, -3f)
		PostEvent(RenderOverlay())
		GlStateManager.translate(0f, 0f, 3f)
		GlStateManager.popMatrix()
	}
	
	@SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
	fun onChatMessage(event: PacketEvent.Received) {
		val packet = event.packet
		
		if (packet is S02PacketChat) {
			if (packet.type.toInt() in 0 .. 1) {
				event.isCanceled = PostEvent(Chat(packet.chatComponent))
			}
			if (packet.type.toInt() == 2) {
				event.isCanceled = PostEvent(Actionbar(packet.chatComponent))
			}
		}
		
		if (packet is S2DPacketOpenWindow) {
			currentInventory = Inventory(
				packet.windowTitle.unformattedText,
				packet.windowId,
				packet.slotCount
			)
			acceptItems = true
		}
		
		if (packet is S2FPacketSetSlot) {
			if (!acceptItems) {
				currentInventory?.let {
					if (it.windowId != packet.func_149175_c()) return
					
					val slot = packet.func_149173_d()
					if (slot < it.slotCount) {
						val itemStack = packet.func_149174_e()
						if (itemStack != null) {
							it.items[slot] = itemStack
						}
					}
				}
				return
			}
			currentInventory?.let {
				if (it.windowId != packet.func_149175_c()) return
				
				val slot = packet.func_149173_d()
				if (slot < it.slotCount) {
					val itemStack = packet.func_149174_e()
					if (itemStack != null) {
						it.items[slot] = itemStack
					}
				} else {
					done(it)
					return
				}
				
				if (it.items.size == it.slotCount) {
					done(it)
				}
			}
		}
		
		if (packet is S32PacketConfirmTransaction) {
			PostEvent(ServerTick())
        }
	}
	
	
	private var currentInventory: Inventory? = null
	private var acceptItems = false

	
	private fun done(inventory: Inventory) {
		PostEvent(InventoryFullyOpenedEvent(inventory))
		acceptItems = false
	}
	
	
	data class Inventory(
		val title: String,
		val windowId: Int,
		val slotCount: Int,
		val items: MutableMap<Int, ItemStack> = mutableMapOf()
	)
}