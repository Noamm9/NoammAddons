package com.github.noamm9.features.impl.dungeon

/*
object BloodRoom: Feature() {
    private val bloodCamp by ToggleSetting("Blood Camp Helper", true)
    private val showTimer by ToggleSetting("Show Timer", true).showIf { bloodCamp.value }
    private val showBox by ToggleSetting("Show Box", true).showIf { bloodCamp.value }
    private val showLine by ToggleSetting("Show Line", true).showIf { bloodCamp.value }
    private val boxColor by ColorSetting("Box Color", Color(255, 0, 255)).showIf { bloodCamp.value && showBox.value }
    private val lineColor by ColorSetting("Line Color", Color.CYAN).showIf { bloodCamp.value && showLine.value }

    private data class BloodEntity(var startVec: Vec3, val start: Long, var firstSpawn: Boolean, var endVec: Vec3?)

    private val bloodMobs = mutableMapOf<ArmorStand, BloodEntity>()
    private var watcherEntity: Zombie? = null
    private var firstSpawns = true

    private val firstSpawnRegex = Regex("^\\[BOSS\\] The Watcher\\: Let\\'s see how you can handle this\\.\$")
    private val watcherIntroRegex = setOf(
        "[BOSS] The Watcher: Congratulations, you made it through the Entrance.",
        "[BOSS] The Watcher: Ah, you've finally arrived.",
        "[BOSS] The Watcher: Ah, we meet again...",
        "[BOSS] The Watcher: So you made it this far... interesting.",
        "[BOSS] The Watcher: You've managed to scratch and claw your way here, eh?",
        "[BOSS] The Watcher: I'm starting to get tired of seeing you around here...",
        "[BOSS] The Watcher: Oh.. hello?",
        "[BOSS] The Watcher: Things feel a little more roomy now, eh?"
    )

    override fun init() {
        register<MainThreadPacketReceivedEvent.Pre> {
            if (! bloodCamp.value) return@register
            if (DungeonListener.bloodOpenTime == null) return@register
            if (DungeonListener.watcherClearTime != null) return@register

            val packet = event.packet as? ClientboundMoveEntityPacket ?: return@register
            if (packet.xa == 0.toShort() && packet.ya == 0.toShort() && packet.za == 0.toShort()) return@register
            val entity = packet.getEntity(mc.level) as? ArmorStand ?: return@register
            if (watcherEntity?.let { it.distanceTo(entity) > 20 } != false) return@register
            val skull = entity.getItemBySlot(EquipmentSlot.HEAD)
            if (skull.isEmpty || ! skull.`is`(Items.PLAYER_HEAD)) return@register
            if (ItemUtils.getSkullTexture(skull) !in mobSkulls) return@register

            val packetVec = Vec3(
                entity.x + packet.xa / 4096.0,
                entity.y + packet.ya / 4096.0,
                entity.z + packet.za / 4096.0
            )

            val data = bloodMobs.getOrPut(entity) {
                BloodEntity(packetVec, DungeonListener.currentTime, firstSpawns, null)
            }.takeUnless { packetVec == it.startVec || it.endVec != null } ?: return@register

            val timeTook = (DungeonListener.currentTime - data.start).toDouble()
            val spawnTime = (if (data.firstSpawn) 78 else 38) - timeTook + 0.8
            val directionVec = packetVec.subtract(data.startVec).multiply(spawnTime / timeTook)
            data.endVec = packetVec.add(directionVec)
        }

        register<RenderWorldEvent> {
            //  if (! bloodCamp.value || DungeonListener.watcherClearTime != null || DungeonListener.bloodOpenTime == null) return@register
            //    if (! (showTimer.value || showLine.value || showBox.value)) return@register

            bloodMobs.entries.removeIf { ! it.key.isAlive }

            bloodMobs.forEach { (entity, bloodMob) ->
                val endVector = bloodMob.endVec ?: return@forEach
                val spawnTime = if (bloodMob.firstSpawn) 78 else 38
                val timeLeft = ((spawnTime - (DungeonListener.currentTime - bloodMob.start) + 0.8) / 20.0)

                val x = endVector.x
                val y = endVector.y + 1.5
                val z = endVector.z

                //    if (showTimer.value) {
                Render3D.renderString(timeLeft.toFixed(1), Vec3(x, y + 0.5, z), scale = 2f, phase = true)
                //   }
                //   if (showLine.value) {
                Render3D.renderLine(event.ctx, entity.renderVec.center().add(y = 1), Vec3(x, y, z), 2f, lineColor.value)
                //   }
                //  if (showBox.value) {
                Render3D.renderBox(event.ctx, x - 0.5, y, z - 0.5, 1.0, 1.0, boxColor.value, outline = true, fill = false, phase = true)
                //  }
            }
        }

        register<WorldChangeEvent> {
            bloodMobs.clear()
            watcherEntity = null
            firstSpawns = true
        }
    }
}*/

import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.modMessage
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.ServerUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.items.ItemUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.NoammRenderLayers
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderContext
import net.minecraft.client.renderer.ShapeRenderer
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.awt.Color

object BloodCamp: Feature("Features for Blood Camping.") {

    private val entityDataMap = HashMap<ArmorStand, EntityData>()
    private val renderDataMap = HashMap<ArmorStand, RenderEData>()
    private var watcherEntity: Zombie? = null
    private var firstSpawns = true

    private var moveTimeSeconds: Float? = null
    private var currentTickTime = 0L
    private var startTime: Long? = null


    private data class RenderEData(
        var currVector: Vec3, var endVector: Vec3, var endVecUpdated: Long, var speedVector: Vec3,
        var lastEndVector: Vec3? = null, var lastPingPoint: Vec3? = null, var lastEndPoint: Vec3? = null,
        var time: Float? = null,
    )

    private data class EntityData(
        var startVector: Vec3,
        val started: Long,
        var firstSpawns: Boolean = true,
        val deltaHistory: ArrayDeque<Vec3> = ArrayDeque(),
        var lastPosition: Vec3 = startVector
    )

    private fun getTime(firstSpawn: Boolean, timeTook: Long) = (if (firstSpawn) 2000 else 0) + 1900 - timeTook + 40

    private val BLOOD_START_REGEX = Regex("^\\[BOSS] The Watcher: (Congratulations, you made it through the Entrance\\.|Ah, you've finally arrived\\.|Ah, we meet again\\.\\.\\.|So you made it this far\\.\\.\\. interesting\\.|You've managed to scratch and claw your way here, eh\\?|I'm starting to get tired of seeing you around here\\.\\.\\.|Oh\\.\\. hello\\?|Things feel a little more roomy now, eh\\?)$")
    private val BLOOD_MOVE_REGEX = Regex("^\\[BOSS] The Watcher: Let's see how you can handle this\\.$")

    override fun init() {
        register<ChatMessageEvent> {
            if (LocationUtils.inBoss) return@register
            val msg = event.unformattedText
            if (BLOOD_START_REGEX.matches(msg)) startTime = DungeonListener.currentTime
            else if (BLOOD_MOVE_REGEX.matches(msg)) {
                firstSpawns = false
                val tickTime = startTime ?: return@register
                val predTicks = when (val moveTicks = DungeonListener.currentTime - tickTime) {
                    in 31 ..< 34 -> 36
                    in 28 ..< 31 -> 33
                    in 25 ..< 28 -> 30
                    in 22 ..< 25 -> 27
                    in 1 ..< 22 -> 24
                    else -> moveTicks + 3
                }

                moveTimeSeconds = predTicks / 20f
                modMessage("Watcher will move in ${moveTimeSeconds?.toFixed(2)}s.")

                ThreadUtils.scheduledTaskServer(predTicks) {
                    ChatUtils.showTitle("Kill Mobs")
                    moveTimeSeconds = null
                }
            }
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            if (watcherEntity != null || LocationUtils.inBoss) return@register
            val packet = event.packet as? ClientboundSetEquipmentPacket ?: return@register
            val itemStack = packet.slots.find { it.first == EquipmentSlot.HEAD }?.second ?: return@register
            if (itemStack.isEmpty || ! itemStack.`is`(Items.PLAYER_HEAD)) return@register
            if (ItemUtils.getSkullTexture(itemStack) !in watcherSkulls) return@register
            watcherEntity = mc.level?.getEntity(packet.entity) as? Zombie
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            if (watcherEntity == null) return@register
            val packet = event.packet as? ClientboundRemoveEntitiesPacket ?: return@register
            if (packet.entityIds.none { it == watcherEntity?.id }) return@register
            watcherEntity = null
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            val packet = event.packet as? ClientboundMoveEntityPacket ?: return@register
            if (packet.xa == 0.toShort() && packet.ya == 0.toShort() && packet.za == 0.toShort()) return@register
            val level = mc.level ?: return@register
            if (LocationUtils.inBoss) return@register

            val entity = packet.getEntity(level) as? ArmorStand ?: return@register
            if (watcherEntity?.let { it.distanceTo(entity) <= 20 } != true) return@register
            val item = entity.getItemBySlot(EquipmentSlot.HEAD).takeIf { it.`is`(Items.PLAYER_HEAD) } ?: return@register
            if (ItemUtils.getSkullTexture(item) !in mobSkulls) return@register

            val packetVec = Vec3(
                entity.x + (packet.xa / 4096),
                entity.y + (packet.ya / 4096),
                entity.z + (packet.za / 4096)
            )

            val data = entityDataMap.getOrPut(entity) { EntityData(packetVec, currentTickTime, firstSpawns, lastPosition = packetVec) }
            val delta = packetVec.subtract(data.lastPosition)
            data.lastPosition = packetVec

            if (delta.lengthSqr() > 0) data.deltaHistory.addLast(delta)

            val totalDelta = data.deltaHistory.fold(Vec3.ZERO) { acc, d -> acc.add(d) }
            val endpoint = data.startVector.add((if (totalDelta.lengthSqr() > 0) totalDelta.normalize() else Vec3.ZERO).scale(if (data.firstSpawns) 16.1 else 11.9))
            val timeTook = currentTickTime - data.started
            val speedVec = Vec3(
                (packetVec.x - data.startVector.x) / timeTook,
                (packetVec.y - data.startVector.y) / timeTook,
                (packetVec.z - data.startVector.z) / timeTook
            )

            renderDataMap.getOrPut(entity) { RenderEData(packetVec, endpoint, currentTickTime, speedVec) }.apply {
                lastEndVector = endVector
                endVecUpdated = currentTickTime
                speedVector = speedVec
                currVector = packetVec
                endVector = endpoint
            }
        }

        register<TickEvent.Server> {
            currentTickTime += 50
            moveTimeSeconds?.let { moveTimeSeconds = it - 0.05f }
        }

        register<WorldChangeEvent> {
            watcherEntity = null
            entityDataMap.clear()
            renderDataMap.clear()
            currentTickTime = 0
            firstSpawns = true
            moveTimeSeconds = null
            startTime = null
        }

        register<RenderWorldEvent> {
            if (LocationUtils.inBoss) return@register
            val boxSize = 0.8
            val boxOffset = Vec3(boxSize / - 2.0, 1.5, boxSize / - 2.0)

            renderDataMap.forEach { (entity, renderData) ->
                if (! entity.isAlive) return@forEach
                val data = entityDataMap[entity] ?: return@forEach
                val (currVector, endVector, _, speedVectors) = renderData

                val timeTook = currentTickTime - data.started
                val time = getTime(data.firstSpawns, timeTook)

                val mobOffset = ServerUtils.averagePing.toFloat()
                val pingPoint = Vec3(entity.x + speedVectors.x * mobOffset, entity.y + speedVectors.y * mobOffset, entity.z + speedVectors.z * mobOffset)

                renderData.lastEndPoint = endVector
                renderData.lastPingPoint = pingPoint

                val pingAABB = AABB(boxSize, boxSize, boxSize, 0.0, 0.0, 0.0).move(boxOffset.add(pingPoint))
                val endAABB = AABB(boxSize, boxSize, boxSize, 0.0, 0.0, 0.0).move(boxOffset.add(endVector))

                if (mobOffset < time) {
                    drawWireFrameBox(event.ctx, pingAABB, Color.RED)
                    drawWireFrameBox(event.ctx, endAABB, Color.cyan)
                }
                else drawWireFrameBox(event.ctx, endAABB, Color.GREEN)

                Render3D.renderLine(event.ctx, currVector.add(y = 2), endVector.add(y = 2), Color.RED)

                val timeDisplay = ((time.toFloat() - 40) / 1000).also { renderData.time = it }
                val colorTime = when {
                    timeDisplay > 1.5 -> 'a'
                    timeDisplay in 0.5 .. 1.5 -> '6'
                    timeDisplay in 0.0 .. 0.5 -> 'c'
                    else -> 'b'
                }

                Render3D.renderString("§$colorTime${timeDisplay.toFixed(2)}s", endVector.add(y = 2), scale = 2f)
            }
        }
    }

    private fun drawWireFrameBox(ctx: RenderContext, aabb: AABB, bboxColor: Color) {
        val cam = ctx.camera.position.reverse()
        ctx.matrixStack.pushPose()
        ctx.matrixStack.translate(cam.x, cam.y, cam.z)

        ShapeRenderer.renderLineBox(
            ctx.matrixStack.last(),
            ctx.consumers.getBuffer(NoammRenderLayers.getLinesThroughWalls(2.0)),
            aabb.minX, aabb.minY, aabb.minZ,
            aabb.maxX, aabb.maxY, aabb.maxZ,
            bboxColor.red / 255f, bboxColor.green / 255f, bboxColor.blue / 255f, 0.7f
        )

        ctx.matrixStack.popPose()
    }

    private val watcherSkulls = setOf(
        "ewogICJ0aW1lc3RhbXAiIDogMTY5NzMwOTQxNzI1NiwKICAicHJvZmlsZUlkIiA6ICJjYjYxY2U5ODc4ZWI0NDljODA5MzliNWYxNTkwMzE1MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJWb2lkZWRUcmFzaDUxODUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTY2MmI2ZmI0YjhiNTg2ZGM0Y2RmODAzYjA0NDRkOWI0MWQyNDVjZGY2NjhkYWIzOGZhNmMwNjRhZmU4ZTQ2MSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
        "ewogICJ0aW1lc3RhbXAiIDogMTcxOTYwNjM1MjMyMiwKICAicHJvZmlsZUlkIiA6ICI3MmY5MTdjNWQyNDU0OTk0YjlmYzQ1YjVhM2YyMjIzMCIsCiAgInByb2ZpbGVOYW1lIiA6ICJUaGF0X0d1eV9Jc19NZSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yNzM5ZDdmNGU2NmE3ZGIyZWE2Y2Q0MTRlNGM0YmE0MWRmN2E5MjQ1NWM5ZmM0MmNhYWIwMTQ2NjVjMzY3YWQ1IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=",
        "ewogICJ0aW1lc3RhbXAiIDogMTcxOTYwNjI5MjgzNiwKICAicHJvZmlsZUlkIiA6ICIzZDIxZTYyMTk2NzQ0Y2QwYjM3NjNkNTU3MWNlNGJlZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJTcl83MUJsYWNrYmlyZCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iZjZlMWU3ZWQzNjU4NmMyZDk4MDU3MDAyYmMxYWRjOTgxZTI4ODlmN2JkN2I1YjM4NTJiYzU1Y2M3ODAyMjA0IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=",
        "ewogICJ0aW1lc3RhbXAiIDogMTY5NzIzODQ0NjgxMiwKICAicHJvZmlsZUlkIiA6ICJmMjc0YzRkNjI1MDQ0ZTQxOGVmYmYwNmM3NWIyMDIxMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJIeXBpZ3NlbCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80Y2VjNDAwMDhlMWMzMWMxOTg0ZjRkNjUwYWJiMzQxMGYyMDM3MTE5ZmQ2MjRhZmM5NTM1NjNiNzM1MTVhMDc3IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=",
        "ewogICJ0aW1lc3RhbXAiIDogMTcxOTYwNjAwOTg2NywKICAicHJvZmlsZUlkIiA6ICJiMGQ0YjI4YmMxZDc0ODg5YWYwZTg2NjFjZWU5NmFhYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJNaW5lU2tpbl9vcmciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjM3ZGQxOGI1OTgzYTc2N2U1NTZkYzY0NDI0YWY0YjlhYmRiNzVkNGM5ZThiMDk3ODE4YWZiYzQzMWJmMGUwOSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
        "ewogICJ0aW1lc3RhbXAiIDogMTcxOTYwNTkyNDIwNSwKICAicHJvZmlsZUlkIiA6ICIzZDIxZTYyMTk2NzQ0Y2QwYjM3NjNkNTU3MWNlNGJlZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJTcl83MUJsYWNrYmlyZCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9mNWYwZDc4ZmUzOGQxZDdmNzVmMDhjZGNmMmExODU1ZDZkYTAzMzdlMTE0YTNjNjNlM2JmM2M2MThiYzczMmIwIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=",
        "ewogICJ0aW1lc3RhbXAiIDogMTU4OTU1MDkyNjM2MSwKICAicHJvZmlsZUlkIiA6ICI0ZDcwNDg2ZjUwOTI0ZDMzODZiYmZjOWMxMmJhYjRhZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJzaXJGYWJpb3pzY2hlIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzUxOTY3ZGI1ZTMxOTk5MTYyNTIwMjE5MDNjZjRlOTk1MmVmN2NlYzIyMGZhYWNhMWJhNzliYWZlNTkzOGJkODAiCiAgICB9CiAgfQp9",
        "ewogICJ0aW1lc3RhbXAiIDogMTcxOTYwNjIxMjc1NSwKICAicHJvZmlsZUlkIiA6ICI2NGRiNmMwNTliOTk0OTM2YTY0M2QwODEwODE0ZmJkMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJUaGVTaWx2ZXJEcmVhbXMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWZkNjFlODA1NWY2ZWU5N2FiNWI2MTk2YThkN2VjOTgwNzhhYzM3ZTAwMzc2MTU3YjZiNTIwZWFhYTJmOTNhZiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
        "ewogICJ0aW1lc3RhbXAiIDogMTcxOTYwNjIzOTU4NiwKICAicHJvZmlsZUlkIiA6ICJhYWZmMDUwYTExOTk0NzM1YjEyNDVlNDk0MGFlZjY4NCIsCiAgInByb2ZpbGVOYW1lIiA6ICJMYXN0SW1tb3J0YWwiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTVjMWRjNDdhMDRjZTU3MDAxYThiNzI2ZjAxOGNkZWY0MGI3ZWE5ZDdiZDZkODM1Y2E0OTVhMGVmMTY5Zjg5MyIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"
    )

    private val mobSkulls = setOf(
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDEwNjQwNTAsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzVhNzk4NjBhY2E3OTk0MDdjMGZhYTEwYjFiYmNmNDI5OThmYWQ0ZWJjZjMxZDdhMjE0MTgwODI2YjRhYzk0ZTEifX19",
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDExODY2MzYsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzQ3NzQ4NzExOTBjODc4YzlhMmM0NDk2YzFlMTAyNTdjNmM0ZWExMzgwN2Q3MmMxNWQ3YWM2YWIzYTdhOWE4ZGMifX19",
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDAyMDM1NzMsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2Y0NjI0YTlhOGM2OWNhMjA0NTA0YWJiMDQzZDQ3NDU2Y2Q5YjA5NzQ5YTM2MzU3NDYyMzAzZjI3NmEyMjlkNCJ9fX0=",
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDExNDUyMjIsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2M5MTllNWI4ZDU2ZjA2MmEyMWQyMjRkZTE0YWY3NzFlMmY1NWQwOWI1OWU3YjA5OWQwOWRhYTU3NTQwYjc5Y2YiLCJtZXRhZGF0YSI6eyJtb2RlbCI6InNsaW0ifX19fQ==",
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDA1MzgzODIsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2E4OWY2MzAzYWY4NTg3NzYxMDkxMmRjMDRiOGIxZTg5NzI0NzUyZjBhN2VlYTA1YWI2NTQ3ZTIyODE3OWMwNmYiLCJtZXRhZGF0YSI6eyJtb2RlbCI6InNsaW0ifX19fQ==",
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDA5ODk1NTgsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzY3MjM3ZWRkYWViZGJiZGFhY2ZhOTEyODg1NTYwY2NkYzY1ZGE5M2I0YzNkNTEzNTMyODY4ZWMyM2JiNWI0NDgifX19",
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDA0OTUwMjgsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2ZmMTg0YzE5ZTcyNTYyM2QzMjgyOGEwYTRlNzQxZTg2ZjEzNWFjNjNkYmM4MjhmZjNjODQ2ODMzOGYzNjgzYiJ9fX0=",
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDEwMzA3NjUsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzVjY2NkNTNmNTE5MWMyOWE5ZGM4ZjAxNzBmYmRjNGU1OWU2NjQ3NmFhZTMzZGUyN2I0NjhmMWRlMWI3Y2YzYjIifX19",
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDA5MTc4NzYsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2I1YmE3NmUwMmNhYjcyZmE3ZDhhYzU0Y2VlYzg0OTk3NmFiMGIwMGEwMTA2OGQ2OGMyNjY3NjZiZjcwYzM5OTcifX19",
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDA3Njk2MTQsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2FhMjNjOGNkZTI5NDNjODQyNDlkZTgzNTFiYzM1NDBiZTVmOGFmYWFiYThiMmNiMDMyZmM1YWNhZDc4YTI2OWIifX19",
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDA4MTg4MDMsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzkxNzFmMzViOGY1MDgxNDJiZDhjNjU0MTdkMGYzMjQxNTNhYjkxNDc3MzllZTRkMTBkZWE3MzNjYzgwZWFhMjAifX19",
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDA5NTY0MjIsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzdkMTJiMmFkZTQxM2E2Y2Q3Y2NhM2M5NWU5NjFiYTlmMGFlNzE2NWZhNDFmYzdiNWQ1ZjA5NGEwMTI0MGM2MDkifX19",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTZjM2UzMWNmYzY2NzMzMjc1YzQyZmNmYjVkOWE0NDM0MmQ2NDNiNTVjZDE0YzljNzdkMjczYTIzNTIifX19",
        "ewogICJ0aW1lc3RhbXAiIDogMTU4OTkyMzE2OTIxMSwKICAicHJvZmlsZUlkIiA6ICJhMmY4MzQ1OTVjODk0YTI3YWRkMzA0OTcxNmNhOTEwYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJiUHVuY2giLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODQyMWJhNWI4ZTM1NzNlZjk3YmViNWI0MGUxNWQxNWIyMGYzMDYzMWM0YzUzMzBjM2RlZGEzMDQ3ZGYwZTkyIgogICAgfQogIH0KfQ==",
        "ewogICJ0aW1lc3RhbXAiIDogMTU4OTkyMzExMjUwMCwKICAicHJvZmlsZUlkIiA6ICJhMmY4MzQ1OTVjODk0YTI3YWRkMzA0OTcxNmNhOTEwYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJiUHVuY2giLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWQyMjc3MmY3NjkwNDVmZGM1YmU4MTlhZDY4YjAxYTk3YWMwNGM2MDg4NmQyY2E3YWZlZTM5YjI4MmY3YTM4MyIKICAgIH0KICB9Cn0=",
        "ewogICJ0aW1lc3RhbXAiIDogMTU4OTkyMzM4Njc5NCwKICAicHJvZmlsZUlkIiA6ICJhMmY4MzQ1OTVjODk0YTI3YWRkMzA0OTcxNmNhOTEwYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJiUHVuY2giLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWQ2N2Y5N2Q3ZjgyMTcyOWJlYjM0YTgyYzNmMTM1OTJiNDA0MzlmZTUyNDhlNzI1NzZmZGU3YWExODBiZjc3IgogICAgfQogIH0KfQ==",
        "ewogICJ0aW1lc3RhbXAiIDogMTU4OTkyMzIxNTkwNSwKICAicHJvZmlsZUlkIiA6ICJhMmY4MzQ1OTVjODk0YTI3YWRkMzA0OTcxNmNhOTEwYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJiUHVuY2giLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmIzOTczYTc1MmIyNGEyZjNhYmIwMDM0MjdmNmRiZTZjYTNhNjFkYjBhMWJjZjM1MWM2ZWFiMjdlYzI3ZTUwIgogICAgfQogIH0KfQ==",
        "eyJ0aW1lc3RhbXAiOjE1NzQ0MTkzMTAxNjQsInByb2ZpbGVJZCI6Ijc1MTQ0NDgxOTFlNjQ1NDY4Yzk3MzlhNmUzOTU3YmViIiwicHJvZmlsZU5hbWUiOiJUaGFua3NNb2phbmciLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzEyNzE2ZWNiZjViOGRhMDBiMDVmMzE2ZWM2YWY2MWU4YmQwMjgwNWIyMWViOGU0NDAxNTE0NjhkYzY1NjU0OWMifX19",
        "ewogICJ0aW1lc3RhbXAiIDogMTU4OTkyMzAyODAxNSwKICAicHJvZmlsZUlkIiA6ICJhMmY4MzQ1OTVjODk0YTI3YWRkMzA0OTcxNmNhOTEwYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJiUHVuY2giLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzI2MDMyNTE3MWE3YmE4NDYwODMwYzBlZWE1MTVjNzU3YTY2NWU1YjE2YTE0MjA3YmExYTMxODI3NTJiZWU4NyIKICAgIH0KICB9Cn0=",
        "ewogICJ0aW1lc3RhbXAiIDogMTU5NTQyODIyMDAyMCwKICAicHJvZmlsZUlkIiA6ICJkYTQ5OGFjNGU5Mzc0ZTVjYjYxMjdiMzgwODU1Nzk4MyIsCiAgInByb2ZpbGVOYW1lIiA6ICJOaXRyb2hvbGljXzIiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjJkOGZkM2FhNTYxN2IxZGFjMGFhZTljODFmNmRkNzBhZDkzYTU5OTQyZjQ2MGQyN2U0ZDU1YTVjYjg5MThlOCIKICAgIH0KICB9Cn0=",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTZmYzg1NGJiODRjZjRiNzY5NzI5Nzk3M2UwMmI3OWJjMTA2OTg0NjBiNTFhNjM5YzYwZTVlNDE3NzM0ZTExIn19fQ==",
        "ewogICJ0aW1lc3RhbXAiIDogMTU4OTc5MzA2ODgzOSwKICAicHJvZmlsZUlkIiA6ICIyYzEwNjRmY2Q5MTc0MjgyODRlM2JmN2ZhYTdlM2UxYSIsCiAgInByb2ZpbGVOYW1lIiA6ICJOYWVtZSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83ZGU3YmJiZGYyMmJmZTE3OTgwZDRlMjA2ODdlMzg2ZjExZDU5ZWUxZGI2ZjhiNDc2MjM5MWI3OWE1YWM1MzJkIgogICAgfQogIH0KfQ==",
        "ewogICJ0aW1lc3RhbXAiIDogMTU5ODk3NzI1OTM1NywKICAicHJvZmlsZUlkIiA6ICJlNzkzYjJjYTdhMmY0MTI2YTA5ODA5MmQ3Yzk5NDE3YiIsCiAgInByb2ZpbGVOYW1lIiA6ICJUaGVfSG9zdGVyX01hbiIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9jMTAwN2M1YjcxMTRhYmVjNzM0MjA2ZDRmYzYxM2RhNGYzYTBlOTlmNzFmZjk0OWNlZGFkYzk5MDc5MTM1YTBiIgogICAgfQogIH0KfQ=="
    )
}