package NoammAddons.events

import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.ThreadUtils.setTimeout
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


object RegisterEvents {
	private fun PostEvent(event: Event) = MinecraftForge.EVENT_BUS.post(event)
	
	
	
	@SubscribeEvent
	fun onRenderOverlay(event: RenderGameOverlayEvent.Pre) {
		if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR) return
		if (mc.renderManager?.fontRenderer == null) return
		
		GlStateManager.pushMatrix()
		GlStateManager.translate(0f, 0f, -3f)
		PostEvent(RenderOverlay())
		GlStateManager.translate(0f, 0f, 3f)
		GlStateManager.popMatrix()
	}
	
	@SubscribeEvent
	fun onChatMessage(event: PacketEvent.Received) {
		if (event.packet !is S02PacketChat) return
		
		if (event.packet.type.toInt() in 0..1) {
			event.isCanceled = PostEvent(Chat(event.packet.chatComponent))
		}
		if (event.packet.type.toInt() == 2) {
			event.isCanceled = PostEvent(Actionbar(event.packet.chatComponent))
		}
	}
	
	
	private var currentInventory: Inventory? = null
	private var acceptItems = false
	
	@SubscribeEvent
	fun onInventoryDataReceiveEvent(event: PacketEvent.Received) {
		val packet = event.packet
		
		if (packet is S2DPacketOpenWindow) {
			val windowId = packet.windowId
			val title = packet.windowTitle.unformattedText
			val slotCount = packet.slotCount
			currentInventory = Inventory(windowId, title, slotCount)
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
	}
	
	private fun done(inventory: Inventory) {
		setTimeout(100) {
			PostEvent(InventoryFullyOpenedEvent(inventory))
			acceptItems = false
		}
	}
	
	
	data class Inventory(
		val windowId: Int,
		val title: String,
		val slotCount: Int,
		val items: MutableMap<Int, ItemStack> = mutableMapOf(),
		var fullyOpenedOnce: Boolean = false,
	)
}