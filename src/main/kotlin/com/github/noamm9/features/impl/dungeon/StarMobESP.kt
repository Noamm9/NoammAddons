package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.NoammAddons.logger
import com.github.noamm9.compat.Enderman
import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.init.DataDownloader
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.dungeons.map.core.RoomState
import com.github.noamm9.utils.dungeons.map.core.UniqueRoom
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.LocationUtils.inBoss
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.arrow.AbstractArrow
import java.awt.Color
import java.util.concurrent.*

object StarMobESP: Feature(
    "Highlights all starred mobs in a dungeon.",
    //#if LEGIT name = "Star Mob Highlight",
    jsonName = "Star Mob ESP"
) {
    private val spawnDetection by ToggleSetting("Spawn Detection", false).apply {
        val lines = buildList {
            add("Highlights mobs the moment they spawn on a known starred mob spawn spot.")
            add("this result in a way faster starred mob detection fully getting the all of the mobs without even fully entering the room")
            add("&eHowever this comes at a cost, non starred mob spawning on a starred mob spot stays highlighted until its nametag loads and corrects it.")
        }

        withDescription(lines.joinToString("\n"))
        section("Settings")
    }
    private val espBats by ToggleSetting("Highlight Bats", true).withDescription("Highlights Bats in Dungeons.")

    private val starMobColor by ColorSetting("Star Mob Color", Color.YELLOW, false).section("General Colors").withDescription("Default color for all Starred mobs.")
    private val felColor by ColorSetting("Fel Color", Color.PINK, false).withDescription("The color used for fels.")
    private val batColor by ColorSetting("Bat Color", Color.GREEN, false).withDescription("The color used for highlighted bats.").showIf { espBats.value }

    private val fakeMiniRooms = setOf("Duncan", "Balcony", "Wizard", "Pit", "Waterfall", "Atlas", "Chambers", "Layers", "Perch", "Golden Oasis") // thanks clear sim
    private val batHealths = setOf(100f, 200f, 400f, 800f)

    private const val DECOY_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTU5NTAxMjc4ODI0MywKICAicHJvZmlsZUlkIiA6ICI4MmM2MDZjNWM2NTI0Yjc5OGI5MWExMmQzYTYxNjk3NyIsCiAgInByb2ZpbGVOYW1lIiA6ICJOb3ROb3RvcmlvdXNOZW1vIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2RiZmUwOThjNzM2NmZmYWJhYzQ4ZjdkNDY1MDI3MjEyZjZhNzdlZGQ3M2FjYWZjYWQyYzA4NzVmMTFhMDMxYjIiCiAgICB9CiAgfQp9"
    private const val SKELETOR_PRIME_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTYwMDc3NTY0NTk5OSwKICAicHJvZmlsZUlkIiA6ICJiZWNkZGIyOGEyYzg0OWI0YTliMDkyMmE1ODA1MTQyMCIsCiAgInByb2ZpbGVOYW1lIiA6ICJTdFR2IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzFiZDNhNGY4ZTIyZGIxNWU0ODZhY2FjNWQ0NTZmZjM1ZjM0ZWQ5YWI1YzY0ZWVjMDU3YWZkMjg2NmQ1NTk0MDEiCiAgICB9CiAgfQp9"

    private val starMobs = HashSet<Int>()
    private val checked = HashSet<Int>()

    private val spotMobs = HashSet<Int>()
    private val notStarred = HashSet<Int>()

    // huge thanks to predever (owner of clear.predever.com dungeon sim) for all the spawn locations
    private val mobSpawns by lazy {
        runCatching { DataDownloader.loadJson<Map<String, List<PackData>>>("mobSpawns.jsonc") }.onFailure {
            logger.error("Failed to load mob spawns", it)
        }.getOrDefault(emptyMap())
    }

    private val roomSpawns = ConcurrentHashMap<UniqueRoom, List<Spot>>()

    override fun init() {
        register<DungeonEvent.RoomEvent.RotationFound> {
            val packs = mobSpawns[event.room.name] ?: return@register
            roomSpawns[event.room] = packs.flatMap { data ->
                val pack = Pack(data.count)
                data.spots.map {
                    val (x, y, z) = it.split(",").map(String::toInt)
                    val roomPos = ScanUtils.getRealCoord(BlockPos(x, y, z), event.corner, 360 - event.rotation)
                    Spot(roomPos, pack)
                }
            }

            if (spawnDetection.value) level.entitiesForRendering().forEach(::checkSpawnSpot)
        }

        register<MainThreadPacketReceivedEvent.Post> {
            if (! LocationUtils.inDungeon || inBoss) return@register

            if (spawnDetection.value && event.packet is ClientboundAddEntityPacket) {
                level.getEntity(event.packet.id)?.let(::checkSpawnSpot)
                return@register
            }

            if (event.packet !is ClientboundSetEntityDataPacket) return@register
            val entity = level.getEntity(event.packet.id) ?: return@register

            if (entity is ArmorStand) {
                val name = entity.customName?.formattedText ?: return@register
                if (! name.endsWith("§c❤")) return@register
                if (name.contains("✯")) checkStarMob(entity, name)
                else checkUnstarredMob(entity, name)
            }
            else if (entity is Player) {
                val name = mc.connection?.getPlayerInfo(entity.uuid)?.profile?.name ?: return@register
                if (! name.equalsOneOf("Shadow Assassin", "Lost Adventurer", "Diamond Guy", "King Midas")) return@register
                val roomName = ScanUtils.getRoomFromPos(entity.position())?.name ?: return@register
                if (roomName in fakeMiniRooms) return@register
                if (roomName == "Stairs" && entity.y > 80) return@register

                starMobs.add(entity.id)
            }
        }

        register<EntityUnloadEvent> {
            if (! LocationUtils.inDungeon || inBoss) return@register
            starMobs.remove(event.entity.id)
            checked.remove(event.entity.id)
            notStarred.remove(event.entity.id)
            spotMobs.remove(event.entity.id)
        }

        register<WorldChangeEvent> {
            starMobs.clear()
            checked.clear()
            roomSpawns.clear()
            spotMobs.clear()
            notStarred.clear()
        }

        register<CheckEntityGlowEvent> {
            if (! LocationUtils.inDungeon || inBoss) return@register

            if (event.entity.id in starMobs) {
                val color = if (event.entity is Enderman/* && event.entity.customName?.string == "Dinnerbone"*/) felColor.value
                else starMobColor.value

                event.color = color
                return@register
            }

            if (! espBats.value) return@register
            if (event.entity !is Bat) return@register
            if (event.entity.maxHealth !in batHealths) return@register
            event.color = batColor.value
        }
    }

    private fun checkStarMob(armorStand: Entity, name: String) {
        if (! checked.add(armorStand.id)) return
        val id = getMobId(armorStand, name)

        val mob = armorStand.level().getEntity(id)
        if (mob !is ArmorStand && (id !in starMobs || id in spotMobs) && mob != null) {
            spotMobs.remove(id)
            starMobs.add(id)
            return
        }

        val possibleEntities = armorStand.level().getEntities(
            armorStand, armorStand.boundingBox.move(0.0, - 1.0, 0.0)
        ) { it !is ArmorStand && it !is ExperienceOrb }

        possibleEntities.find {
            ! starMobs.contains(it.id) && when (it) {
                is Player -> ! it.isInvisible && it.uuid.version() == 2 && it != player
                is WitherBoss -> false
                is AbstractArrow -> false
                else -> true
            }
        }?.let {
            starMobs.add(it.id)
        }
    }

    private fun checkUnstarredMob(armorStand: Entity, name: String) {
        if (! checked.add(armorStand.id)) return
        val id = getMobId(armorStand, name)
        val mob = armorStand.level().getEntity(id)
        if (mob == null || mob is ArmorStand) return

        notStarred.add(id)
        if (spotMobs.remove(id)) starMobs.remove(id)
    }

    private fun checkSpawnSpot(entity: Entity) {
        if (! spawnDetection.value) return
        if (entity is Bat || entity is ArmorStand) return
        if (entity is Player && entity.uuid.version() != 2) return
        if (entity !is Mob && entity !is Player) return
        if (entity.id in starMobs || entity.id in notStarred) return
        if (excludeEntity(entity)) return
        val room = ScanUtils.getRoomFromPos(entity.position()) ?: return
        if (room.mainRoom.state.ordinal <= RoomState.CLEARED.ordinal) return
        val spots = roomSpawns[room] ?: return

        val pos = entity.blockPosition()
        val spot = spots.minByOrNull { it.pos.distSqr(pos) } ?: return
        if (spot.pos.distSqr(pos) > 2) return
        if (entity.id !in spot.pack.mobs && spot.pack.mobs.size >= spot.pack.count) return
        spot.pack.mobs.add(entity.id)
        starMobs.add(entity.id)
        spotMobs.add(entity.id)
    }

    fun excludeEntity(entity: Entity): Boolean {
        if (entity !is Player) return false
        val name = entity.gameProfile.name
        val skin = entity.gameProfile.properties["textures"].firstOrNull()?.value

        // not a typo just the name has a space. thx hypixel
        if (name == "Decoy " && skin == DECOY_TEXTURE) return true
        if (name == "Skeletor Prime" && skin == SKELETOR_PRIME_TEXTURE) return true

        return false
    }

    private fun getMobId(armorStand: Entity, name: String): Int {
        // withermancers are always -3 to real entity the -1 and -2 are the wither skulls that they shoot
        val offset = if (name.removeFormatting().uppercase().contains("WITHERMANCER")) 3 else 1
        return armorStand.id - offset
    }

    private data class Spot(val pos: BlockPos, val pack: Pack)
    private class Pack(val count: Int) {
        val mobs = HashSet<Int>()
    }

    private data class PackData(val count: Int, val spots: List<String>)
}