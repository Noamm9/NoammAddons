package noammaddons

import com.google.gson.*
import net.minecraft.client.gui.GuiDownloadTerrain
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityList
import net.minecraft.item.ItemStack
import net.minecraft.nbt.*
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraft.network.play.server.S0FPacketSpawnMob
import net.minecraft.util.BlockPos
import net.minecraft.util.Rotations
import net.minecraftforge.event.entity.player.AttackEntityEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import noammaddons.NoammAddons.Companion.mc
import noammaddons.events.*
import noammaddons.features.impl.dungeons.dmap.core.map.Room
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonInfo
import noammaddons.features.impl.dungeons.dragons.WitherDragonEnum
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.DungeonUtils.dungeonTeammates
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.LocationUtils.onHypixel
import noammaddons.utils.ScanUtils
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.send


// used to be a place for me to test shit.
// but now it's just a dump of silent features/fixes

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
        val packet = event.packet as? C01PacketChatMessage ?: return
        if (fuckingBitch) {
            if (packet.message != "/locraw") return
            debugMessage("Cancelling /locraw")
            event.isCanceled = true
        }

        if (event.packet.message == "/odingetpingcommand-----") {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onPlaterInteract(e: AttackEntityEvent) {
        if (! inDungeon) return
        if (e.entityPlayer != mc.thePlayer) return
        if (dungeonTeammates.none { it.entity == e.target }) return
        e.isCanceled = true
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

    @SubscribeEvent
    fun dragonSpawn(event: PacketEvent.Received) {
        val packet = event.packet as? S0FPacketSpawnMob ?: return
        if (packet.entityType != 63) return
    }


    fun serializeEntityToJson_1_8_9(entity: Entity, type: WitherDragonEnum): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val rootJson = JsonObject()

        rootJson.addProperty("M7DragonType", type.name)
        rootJson.addProperty("entityType", EntityList.getEntityString(entity))
        rootJson.addProperty("entityId", entity.entityId)
        rootJson.addProperty("entityUUID", entity.uniqueID.toString())

        val nbtData = NBTTagCompound()
        entity.writeToNBT(nbtData)
        rootJson.add("nbt", convertNbtToJson_1_8_9(nbtData))

        val dataWatcherJson = JsonObject()
        entity.dataWatcher.allWatched.forEach { watchedObject ->
            if (watchedObject != null) {
                val type = when (watchedObject.objectType) {
                    0 -> Byte::class.simpleName
                    1 -> Short::class.simpleName
                    2 -> Int::class.simpleName
                    3 -> Float::class.simpleName
                    4 -> String::class.simpleName
                    5 -> ItemStack::class.simpleName
                    6 -> BlockPos::class.simpleName
                    7 -> Rotations::class.simpleName
                    else -> "null"
                }
                val objectId = "($type) ${watchedObject.dataValueId}"
                val value = watchedObject.getObject()
                dataWatcherJson.addProperty(objectId, value?.toString() ?: "null")
            }
        }
        rootJson.add("dataWatcher", dataWatcherJson)

        return gson.toJson(rootJson)
    }

    private fun convertNbtToJson_1_8_9(tag: NBTBase): JsonElement {
        return when (tag) {
            is NBTTagCompound -> {
                val jsonObject = JsonObject()
                tag.keySet.forEach { jsonObject.add(it, convertNbtToJson_1_8_9(tag.getTag(it))) }
                jsonObject
            }

            is NBTTagList -> {
                val jsonArray = JsonArray()
                for (i in 0 until tag.tagCount()) {
                    jsonArray.add(convertNbtToJson_1_8_9(tag.get(i)))
                }
                jsonArray
            }

            is NBTTagString -> JsonPrimitive(tag.string)
            is NBTTagByte -> JsonPrimitive(tag.byte)
            is NBTTagShort -> JsonPrimitive(tag.short)
            is NBTTagInt -> JsonPrimitive(tag.int)
            is NBTTagLong -> JsonPrimitive(tag.long)
            is NBTTagFloat -> JsonPrimitive(tag.float)
            is NBTTagDouble -> JsonPrimitive(tag.double)
            is NBTTagByteArray -> JsonArray().apply { tag.byteArray.forEach { add(JsonPrimitive(it)) } }
            is NBTTagIntArray -> JsonArray().apply { tag.intArray.forEach { add(JsonPrimitive(it)) } }
            else -> JsonPrimitive("Unsupported Tag ID: ${tag.id}")
        }
    }
}