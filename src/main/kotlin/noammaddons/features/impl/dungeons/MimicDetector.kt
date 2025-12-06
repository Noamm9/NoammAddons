package noammaddons.features.impl.dungeons

import gg.essential.elementa.state.BasicState
import gg.essential.elementa.utils.withAlpha
import net.minecraft.entity.monster.EntityZombie
import net.minecraft.network.play.server.S19PacketEntityStatus
import net.minecraft.tileentity.TileEntityChest
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.features.impl.dungeons.dmap.core.map.UniqueRoom
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonInfo
import noammaddons.ui.config.core.annotations.AlwaysActive
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.websocket.WebSocket
import noammaddons.websocket.packets.S2CPacketDungeonMimic
import noammaddons.websocket.packets.S2CPacketDungeonPrince
import java.awt.Color

@AlwaysActive
object MimicDetector: Feature("Detects when a Mimic or Prince is killed") {
    private val mimic = ToggleSetting("Send Mimic Message", true)
    private val prince = ToggleSetting("Send Prince Message", true)
    private val highlightChest = ToggleSetting("Highlight Chest", false)
    private val highlightColor = ColorSetting("Highlight Color", Color.RED.withAlpha(0.3f))
        .addDependency(highlightChest)

    override fun init() = addSettings(
        mimic, prince,
        SeperatorSetting("Highlight"),
        highlightChest, highlightColor
    )

    val shouldScanMimic get() = ! mimicKilled.get() && inDungeon && (dungeonFloorNumber ?: 0) >= 6 && ! inBoss
    val mimicKilled = BasicState(false)
    val princeKilled = BasicState(false)

    var mimicRoom: UniqueRoom? = null

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        mimicKilled.set(false)
        princeKilled.set(false)
        mimicRoom = null
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! inDungeon) return
        val msg = event.component.noFormatText.lowercase()

        if (! princeKilled.get() && princeMessages.any { msg.contains(it) }) {
            princeKilled.set(true)
            if (DungeonUtils.dungeonTeammatesNoSelf.isNotEmpty()) {
                WebSocket.send(S2CPacketDungeonPrince())
            }
            if (enabled && prince.value && msg == "a prince falls. +1 bonus score") {
                ChatUtils.sendPartyMessage("Prince Killed")
            }
        }

        if (shouldScanMimic && mimicMessages.any { msg.contains(it) }) {
            mimicKilled.set(true)
        }
    }

    @SubscribeEvent
    fun onLivingDeath(event: LivingDeathEvent) {
        if (! inDungeon || mimicKilled.get()) return
        val entity = event.entity as? EntityZombie ?: return
        if (entity.isChild && (0 .. 3).all { entity.getCurrentArmor(it) == null }) {
            mimicKilled.set(true)
            if (enabled && mimic.value) ChatUtils.sendPartyMessage("Mimic killed!")
            if (DungeonUtils.dungeonTeammatesNoSelf.isNotEmpty()) WebSocket.send(S2CPacketDungeonMimic())
        }
    }

    @SubscribeEvent
    fun onEntityDeath(event: MainThreadPacketRecivedEvent) {
        if (! inDungeon || mimicKilled.get()) return
        val packet = event.packet as? S19PacketEntityStatus ?: return
        if (packet.opCode.toInt() != 3) return
        if ((packet.getEntity(mc.theWorld) as? EntityZombie)?.takeIf { it.isChild } != null) {
            mimicKilled.set(true)
            if (enabled && mimic.value) ChatUtils.sendPartyMessage("Mimic killed!")
            if (DungeonUtils.dungeonTeammatesNoSelf.isNotEmpty()) WebSocket.send(S2CPacketDungeonMimic())
        }

    }

    @SubscribeEvent
    fun onRenderChestPre(event: RenderChestEvent.Pre) {
        if (shouldHighlightMimicChest(event.chest))
            RenderHelper.enableChums()
    }

    @SubscribeEvent
    fun onRenderChestPost(event: RenderChestEvent.Post) {
        if (! shouldHighlightMimicChest(event.chest)) return
        RenderHelper.disableChums()
        RenderUtils.drawBlockBox(
            event.chest.pos,
            highlightColor.value,
            outline = false, fill = true, phase = true
        )
    }

    private fun shouldHighlightMimicChest(chest: TileEntityChest): Boolean {
        return enabled && highlightChest.value && shouldScanMimic && chest.chestType == 1
                && ScanUtils.getRoomFromPos(chest.pos)?.uniqueRoom == mimicRoom && mimicRoom != null
    }

    fun findMimicRoom(): UniqueRoom? {
        mc.theWorld.loadedTileEntityList.filter { it is TileEntityChest && it.chestType == 1 }
            .groupingBy { ScanUtils.getRoomFromPos(it.pos)?.data?.name }.eachCount().forEach { (room, trappedChests) ->
                DungeonInfo.uniqueRooms.find { it.name == room && it.mainRoom.data.trappedChests < trappedChests }
                    ?.let {
                        it.hasMimic = true
                        mimicRoom = it
                        return it
                    }
            }
        return null
    }

    private val mimicMessages = setOf(
        "mimic dead!", "mimic dead", "mimic killed!", "mimic killed",
        "\$skytils-dungeon-score-mimic$", "child destroyed!", "mimic obliterated!",
        "mimic exorcised!", "mimic destroyed!", "mimic annhilated!",
        "breefing killed", "breefing dead"
    )

    private val princeMessages = setOf(
        "prince dead", "prince dead!", "\$skytils-dungeon-score-prince\$",
        "prince killed", "prince slain", "prince killed!",
        "a prince falls. +1 bonus score"
    )
}