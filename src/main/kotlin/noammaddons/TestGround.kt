package noammaddons

import net.minecraft.client.gui.GuiDownloadTerrain
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraft.network.play.server.*
import net.minecraftforge.event.entity.player.AttackEntityEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import noammaddons.NoammAddons.Companion.mc
import noammaddons.events.*
import noammaddons.features.impl.dungeons.dmap.core.map.Room
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonInfo
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.removeUnicode
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.LocationUtils.onHypixel
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.send


// used to be a place for me to test shit.
// but now it's just a dump of silent features

object TestGround {
    private var fuckingBitch = false
    private var sent = false
    private var a = false

    @SubscribeEvent
    fun handlePartyCommands(event: MessageSentEvent) {
        if (! onHypixel) return
        val msg = event.message.removeFormatting().lowercase()

        if (msg == "/p invite accept") {
            event.isCanceled = true
            a = true
            sent = false
            setTimeout(250) { a = false }
            return
        }

        if (a && ! sent) {
            if (msg.startsWith("/p invite ") || msg.startsWith("/party accept ")) {
                event.isCanceled = true

                val modifiedMessage = msg
                    .replace("/party accept ", "/p join ")
                    .replace("/p invite ", "/p join ")

                sendChatMessage(modifiedMessage)
                sent = true
            }
        }

        if (msg.equalsOneOf("/pll", "/pl")) {
            event.isCanceled = true
            C01PacketChatMessage("/pl").send()
        }
    }

    @SubscribeEvent
    fun wtf(event: WorldLoadPostEvent) {
        setTimeout(500) {
            if (mc.currentScreen !is GuiDownloadTerrain) return@setTimeout
            mc.currentScreen = null
        }
    }

    @SubscribeEvent
    fun fucklocraw(event: FMLNetworkEvent.ClientConnectedToServerEvent) {
        if (event.isLocal) return
        fuckingBitch = true
        setTimeout(5000) { fuckingBitch = false }
    }

    @SubscribeEvent
    fun fuckLocraw2(event: PacketEvent.Sent) {
        if (! fuckingBitch) return
        val packet = event.packet as? C01PacketChatMessage ?: return
        if (packet.message != "/locraw") return
        debugMessage("Cancelling /locraw")
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onPlaterInteract(e: AttackEntityEvent) {
        if (! inDungeon) return
        if (e.entityPlayer != mc.thePlayer) return
        if (DungeonUtils.dungeonTeammates.none { it.entity == e.target }) return
        e.isCanceled = true
    }

    private var recentlyRemovedEntities = ArrayDeque<Int>()
    private var recentlySpawnedEntities = ArrayDeque<Int>()
    private val hiddenEntityIds = mutableListOf<Int>()

    @SubscribeEvent
    fun onWorldChange(event: WorldUnloadEvent) {
        recentlyRemovedEntities = ArrayDeque()
        recentlySpawnedEntities = ArrayDeque()
        hiddenEntityIds.clear()
    }

    @SubscribeEvent
    fun onPacketReceive(event: PacketEvent.Received) {
        if (! inSkyblock) return
        //if (!config.fixGhostEntities) return
        if (ScoreboardUtils.sidebarLines.any { removeUnicode(it.removeFormatting()).contains("Kuudra's Hollow", true) }) return

        when (val packet = event.packet) {
            is S0CPacketSpawnPlayer -> {
                if (packet.entityID in recentlyRemovedEntities) {
                    hiddenEntityIds.add(packet.entityID)
                }
                recentlySpawnedEntities.addLast(packet.entityID)
            }

            is S0FPacketSpawnMob -> {
                if (packet.entityID in recentlyRemovedEntities) {
                    hiddenEntityIds.add(packet.entityID)
                }
                recentlySpawnedEntities.addLast(packet.entityID)
            }

            is S13PacketDestroyEntities -> {
                for (entityID in packet.entityIDs) {
                    if (entityID !in recentlySpawnedEntities) {
                        recentlyRemovedEntities.addLast(entityID)
                        if (recentlyRemovedEntities.size == 10) {
                            recentlyRemovedEntities.removeFirst()
                        }
                    }
                    hiddenEntityIds.remove(entityID)
                }
            }
        }
    }

    @SubscribeEvent
    fun onCheckRender(event: RenderEntityEvent) {
        if (event.entity is EntityArmorStand) {
            with(event.entity) {
                if (ticksExisted < 10 && (0 .. 3).map { getCurrentArmor(it) }.all { it == null } && ! hasCustomName() && nbtTagCompound == null) {
                    event.isCanceled = true
                    return
                }
            }
        }
        if ((event.entity is EntityMob || event.entity is EntityPlayer)) {
            with(event.entity) {
                if (hiddenEntityIds.contains(entityId)) {
                    event.isCanceled = true
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (event.room.rotation != null) return
        DungeonInfo.dungeonList.filterIsInstance<Room>().filterNot { it.isSeparator }.forEach {
            if (it.rotation != null) return@forEach
            it.highestBlock = ScanUtils.gethighestBlockAt(it.x, it.z)
            it.findRotation()
            it.findRotation()
        }
    }
}
