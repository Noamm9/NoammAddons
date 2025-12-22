package noammaddons.events

import gg.essential.api.EssentialAPI
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.item.EntityItem
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.*
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.*
import net.minecraftforge.client.event.sound.PlaySoundEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.*
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase
import noammaddons.NoammAddons.Companion.Logger
import noammaddons.NoammAddons.Companion.mc
import noammaddons.NoammAddons.Companion.scope
import noammaddons.features.impl.dungeons.dmap.core.map.UniqueRoom
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.ChatUtils
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.DungeonUtils.dungeonItemDrops
import noammaddons.utils.DungeonUtils.isSecret
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.RenderHelper.partialTicks
import noammaddons.utils.ScanUtils
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.equalsOneOf


object EventDispatcher {
    data class Inventory(
        val title: String,
        val windowId: Int,
        val slotCount: Int,
        val items: MutableMap<Int, ItemStack?> = mutableMapOf(),
    )

    private var currentInventory: Inventory? = null
    private var acceptItems = false

    private var awaitS32 = false


    /**
     * Posts an event to the event bus and catches any errors.
     * @author Skytils
     */
    private fun Event.postCatch(): Boolean {
        return runCatching {
            MinecraftForge.EVENT_BUS.post(this)
        }.onFailure {
            it.printStackTrace()
            Logger.error("An error occurred ${it.message}", it)
            ChatUtils.clickableChat(
                "Caught and logged an ${it::class.simpleName ?: "error"} at ${this::class.simpleName}. Please report this!, Error: ${it.message}",
                "", it.stackTrace.take(30).joinToString("\n")
            )
        }.getOrDefault(isCanceled)
    }

    @JvmStatic
    fun postAndCatch(event: Event): Boolean = event.postCatch()


    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    fun onPacket(event: PacketEvent.Sent) {
        when (val packet = event.packet) {
            is C08PacketPlayerBlockPlacement -> {
                if (packet.position == null) return
                if (! inDungeon) return
                if (inBoss) return
                if (! isSecret(packet.position)) return

                val type = when (getBlockAt(packet.position)) {
                    Blocks.chest, Blocks.trapped_chest -> DungeonEvent.SecretEvent.SecretType.CHEST
                    Blocks.lever -> DungeonEvent.SecretEvent.SecretType.LAVER
                    Blocks.skull -> DungeonEvent.SecretEvent.SecretType.SKULL
                    else -> return
                }

                DungeonEvent.SecretEvent(type, packet.position).postCatch()
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onEvent(event: Event) {
        when (event) {
            is ClientTickEvent -> {
                if (event.phase != Phase.START) return
                if (mc.theWorld == null || mc.thePlayer == null) return
                if (mc.isSingleplayer) ServerTick().postCatch()
                Tick().postCatch()
            }

            is RenderGameOverlayEvent.Text -> {
                RenderOverlay(partialTicks).postCatch()
                if (EssentialAPI.getMinecraftUtil().isDevelopment())
                    RenderOverlayNoCaching(partialTicks).postCatch()
            }

            is RenderWorldLastEvent -> {
                GlStateManager.pushMatrix()
                RenderWorld(event.partialTicks).postCatch()
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

                soundEvent.postCatch()

                event.result = if (soundEvent.isCanceled) null else event.result
            }

            is WorldEvent.Unload -> WorldUnloadEvent().postCatch()

            is WorldEvent.Load -> awaitS32 = ! mc.isSingleplayer

            is PreKeyInputEvent -> event.isCanceled = UserInputEvent(false, keyCode = event.key).postCatch()

            is GuiKeybourdInputEvent -> event.isCanceled = UserInputEvent(false, event.gui, event.keyCode).postCatch()


            is MouseEvent -> {
                if (event.button == - 1) return
                if (! event.buttonstate) return
                event.isCanceled = UserInputEvent(true, keyCode = event.button).postCatch()
            }

            is GuiMouseClickEvent -> {
                if (event.button == - 1) return
                event.isCanceled = UserInputEvent(true, event.gui, event.button).postCatch()
            }

            is EntityLeaveWorldEvent -> {
                if (! inDungeon) return
                if (inBoss) return
                if (event.entity !is EntityItem) return
                if (event.entity.entityItem?.displayName?.removeFormatting() !in dungeonItemDrops) return
                if (mc.thePlayer.getDistanceToEntity(event.entity) > 6) return

                val type = DungeonEvent.SecretEvent.SecretType.ITEM
                val pos = event.entity.position
                DungeonEvent.SecretEvent(type, pos).postCatch()
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    fun onPacket(event: PacketEvent.Received) {
        when (val packet = event.packet) {
            is S02PacketChat -> when (packet.type.toInt()) {
                in 0 .. 1 -> event.isCanceled = Chat(packet.chatComponent).postCatch()
                2 -> event.isCanceled = Actionbar(packet.chatComponent).postCatch()
            }

            is S32PacketConfirmTransaction -> {
                if (packet.func_148888_e()) return
                if (awaitS32) awaitS32 = false
                ServerTick().postCatch()
            }

            is S03PacketTimeUpdate -> {
                if (! awaitS32) return
                scope.launch {
                    repeat(20) {
                        ServerTick().postCatch()
                        delay(50)
                    }
                }
            }

            is S23PacketBlockChange -> BlockChangeEvent(packet.blockPosition, packet.blockState.block, getBlockAt(packet.blockPosition)).postCatch()
            is S22PacketMultiBlockChange -> packet.changedBlocks.forEach {
                BlockChangeEvent(it.pos, it.blockState.block, getBlockAt(it.pos)).postCatch()
            }

            is S2EPacketCloseWindow -> currentInventory = null

            is S2DPacketOpenWindow -> {
                acceptItems = true
                currentInventory = Inventory(
                    packet.windowTitle.formattedText,
                    packet.windowId,
                    packet.slotCount
                )
            }

            is S2FPacketSetSlot -> {
                if (! acceptItems) return

                currentInventory?.run {
                    if (windowId != packet.func_149175_c()) return
                    val slot = packet.func_149173_d()
                    val itemStack = packet.func_149174_e()

                    if (slot < slotCount) {
                        if (itemStack != null) {
                            items[slot] = itemStack
                        }
                    }
                    else {
                        acceptItems = false
                        setTimeout(20) { InventoryFullyOpenedEvent(title, windowId, slotCount, items).postCatch() }
                        return
                    }

                    if (items.size != slotCount) return@run

                    acceptItems = false
                    setTimeout(20) { InventoryFullyOpenedEvent(title, windowId, slotCount, items).postCatch() }
                }
            }

            is S29PacketSoundEffect -> {
                if (! inDungeon) return
                if (inBoss) return
                if (! packet.soundName.equalsOneOf("mob.bat.hurt", "mob.bat.death")) return
                if (packet.volume != 0.1f) return

                val type = DungeonEvent.SecretEvent.SecretType.BAT
                val pos = BlockPos(packet.x, packet.y, packet.z)
                DungeonEvent.SecretEvent(type, pos).postCatch()
            }
        }
    }

    fun checkForRoomChange(currentRoom: UniqueRoom?, lastKnownRoom: UniqueRoom?) {
        lastKnownRoom?.let {
            DungeonEvent.RoomEvent.onExit(it).postCatch()
        }
        currentRoom?.let {
            it.highestBlock = ScanUtils.gethighestBlockAt(it.mainRoom.x, it.mainRoom.z)
            it.findRotation()
            DungeonEvent.RoomEvent.onEnter(it).postCatch()
        }
    }
}