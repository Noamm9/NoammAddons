package noammaddons.events

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.network.play.server.*
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.client.event.sound.PlaySoundEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noammaddons.noammaddons.Companion.Logger
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.isNull


object RegisterEvents {
    private var currentInventory: Inventory? = null
    private var acceptItems = false


    /**
     * Posts an event to the event bus and catches any errors.
     * @author Skytils
     */
    private fun Event.postCatch(): Boolean {
        return runCatching {
            MinecraftForge.EVENT_BUS.post(this)
        }.onFailure {
            it.printStackTrace()
            Logger.error("An error occurred ${it.message}")
            modMessage("Caught and logged an ${it::class.simpleName ?: "error"} at ${this::class.simpleName}. Please report this!, Error: ${it.message}")
        }.getOrDefault(isCanceled)
    }

    @JvmStatic
    fun postAndCatch(event: Event): Boolean = event.postCatch()

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onTick(event: Event) {
        when (event) {
            is ClientTickEvent -> {
                if (event.phase != TickEvent.Phase.END) return
                if (mc.theWorld.isNull() || mc.thePlayer.isNull()) return
                postAndCatch(Tick())
            }

            is RenderGameOverlayEvent.Text -> {
                if (mc.renderManager?.fontRenderer == null) return
                GlStateManager.pushMatrix()
                GlStateManager.translate(0f, 0f, - 3f)
                postAndCatch(RenderOverlay())
                GlStateManager.translate(0f, 0f, 3f)
                GlStateManager.popMatrix()
            }

            is RenderWorldLastEvent -> {
                GlStateManager.pushMatrix()
                postAndCatch(RenderWorld(event.partialTicks))
                GlStateManager.popMatrix()
            }

            is PlaySoundEvent -> {
                val soundEvent = SoundPlayEvent(
                    event.name,
                    event.sound.volume,
                    event.sound.pitch,
                    Vec3(
                        event.sound.xPosF.toDouble(),
                        event.sound.yPosF.toDouble(),
                        event.sound.zPosF.toDouble()
                    )
                )
                postAndCatch(soundEvent)

                event.result = if (soundEvent.isCanceled) null else event.result
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    fun onPacket(event: PacketEvent.Received) {
        when (val packet = event.packet) {
            is S02PacketChat -> when (packet.type.toInt()) {
                in 0 .. 1 -> event.isCanceled = postAndCatch(Chat(packet.chatComponent))
                2 -> event.isCanceled = postAndCatch(Actionbar(packet.chatComponent))
            }

            is S32PacketConfirmTransaction -> {
                if (packet.func_148888_e()) return
                if (packet.actionNumber > 0) return

                postAndCatch(ServerTick())
            }

            is S23PacketBlockChange -> postAndCatch(BlockChangeEvent(packet.blockPosition, packet.blockState))
            is S22PacketMultiBlockChange -> packet.changedBlocks.forEach {
                postAndCatch(BlockChangeEvent(it.pos, it.blockState))
            }

            is S2EPacketCloseWindow -> currentInventory = null

            is S2DPacketOpenWindow -> {
                currentInventory = Inventory(
                    packet.windowTitle.formattedText,
                    packet.windowId,
                    packet.slotCount
                )
                acceptItems = true
            }

            is S2FPacketSetSlot -> {
                if (! acceptItems) return

                currentInventory?.run {
                    if (windowId != packet.func_149175_c()) return
                    val slot = packet.func_149173_d()

                    if (slot < slotCount) {
                        val itemStack = packet.func_149174_e()
                        if (itemStack != null) {
                            items[slot] = itemStack
                        }
                    }
                    else {
                        acceptItems = false
                        setTimeout(20) { postAndCatch(InventoryFullyOpenedEvent(title, windowId, slotCount, items)) }
                        return
                    }

                    if (items.size != slotCount) return@run

                    acceptItems = false
                    setTimeout(20) { postAndCatch(InventoryFullyOpenedEvent(title, windowId, slotCount, items)) }
                }
            }

            /*
            is S1CPacketEntityMetadata -> {
                val parsedData = mutableMapOf<Int, Any?>()
                val packetData = packet.func_149376_c() ?: return

                try {
                    for (metadata in packetData.filterNotNull()) {
                        parsedData[metadata.dataValueId] = metadata.getObject()
                    }
                }
                catch (_: Exception) {
                }
                finally {
                    event.isCanceled = postAndCatch(
                        EntityMetadataEvent(
                            entity = packet.entityId,
                            flag = (parsedData[0] as Byte?)?.toInt(),
                            airSupply = (parsedData[1] as Short?)?.toInt(),
                            name = parsedData[2] as String?,
                            customNameVisible = parsedData[3],
                            isSilent = parsedData[4],
                            noGravity = parsedData[5],
                            health = parsedData[6] as Float?,
                            potionEffectColor = parsedData[7],
                            isInvisible = parsedData[8],
                            arrowsStuck = parsedData[9],
                            unknown = parsedData.filterKeys { it !in 0 .. 10 },
                            Data = parsedData
                        )
                    )
                }
            }*/
        }
    }

}