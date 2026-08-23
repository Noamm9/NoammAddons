package com.github.noamm9.features.impl.floor7

import com.github.noamm9.NoammAddons
import com.github.noamm9.config.types.TextInputSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.IClientboundMoveEntityPacket
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.MathUtils.aabb
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.MathUtils.destructured
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.RenderHelper.width
import com.github.noamm9.utils.render.world.Render3D.renderBoxBounds
import com.github.noamm9.utils.render.world.Render3D.renderString
import net.minecraft.network.protocol.game.*
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.phys.AABB
import java.awt.Color
import kotlin.math.min

object LeapCounter: Feature("Shows how many players have leaped you") {
    private val alertComplete by ToggleSetting("Alert Complete", true)
    private val completeText by TextInputSetting("Complete Text", "&aEveryone Leaped!").showIf { alertComplete.value }
    private val completeSound = createSoundSettings("Complete Sound", SoundEvents.EXPERIENCE_ORB_PICKUP) { alertComplete.value }
    private var currentSpot: REGION? = null

    override fun init() {
        hudElement("LeapCounter", centered = true) { ctx, e ->
            val region = if (e) REGION.HEE2_BOX else currentSpot ?: return@hudElement 0f to 0f
            val max = if (e) region._maxCount else region.maxCount.takeIf { it > 0 } ?: return@hudElement 0f to 0f
            val startFormat = if (region.maxCount - region.count <= 1) "§9" else "§4"
            val str = "$startFormat${region.count}§9/$max Players Leaped"
            ctx.drawCenteredString(str, 0, 0)
            str.width().toFloat() to 9f
        }

        register<MainThreadPacketReceivedEvent.Post> {
            if (! LocationUtils.inBoss || LocationUtils.dungeonFloorNumber != 7) return@register
            val packet = event.packet
            if (packet !is ClientboundSetEntityMotionPacket && packet !is ClientboundTeleportEntityPacket
                && packet !is ClientboundAddEntityPacket && packet !is ClientboundMoveEntityPacket
                && packet !is ClientboundEntityPositionSyncPacket
            ) return@register

            currentSpot = REGION.entries.find { it.box.contains(player.position()) && ! it.completed } ?: run {
                currentSpot = null
                return@register
            }

            val id = when (packet) {
                is ClientboundSetEntityMotionPacket -> packet.id
                is ClientboundTeleportEntityPacket -> packet.id
                is ClientboundAddEntityPacket -> packet.id
                is ClientboundMoveEntityPacket -> (packet as IClientboundMoveEntityPacket).entityId
                is ClientboundEntityPositionSyncPacket -> packet.id
                else -> return@register
            }

            if (DungeonListener.dungeonTeammatesNoSelf.none { it.entity?.id == id }) return@register

            val (x, y, z) = when (packet) {
                is ClientboundSetEntityMotionPacket -> level.getEntity(id)?.position()?.destructured()
                is ClientboundTeleportEntityPacket -> packet.change.position.destructured()
                is ClientboundAddEntityPacket -> Triple(packet.x, packet.y, packet.z)
                is ClientboundMoveEntityPacket -> packet.getEntity(level)?.positionCodec?.decode(packet.getXa().toLong(), packet.getYa().toLong(), packet.getZa().toLong())?.destructured()
                is ClientboundEntityPositionSyncPacket -> packet.values.position().destructured()
                else -> null
            } ?: return@register

            currentSpot?.updateCounter(id, x, y, z)
        }

        register<RenderWorldEvent> {
            if ("lcbox" in NoammAddons.debugFlags) REGION.entries.forEach {
                event.ctx.renderString(it.name, it.box.center.add(y = 2), scale = 2)
                event.ctx.renderBoxBounds(it.box, Color.YELLOW.withAlpha(100))
            }
        }

        register<WorldChangeEvent> { REGION.reset() }
    }

    private enum class REGION(val box: AABB, val _maxCount: Int, val check: (x: Double, y: Double, z: Double) -> Boolean) {
        SS_BOX(aabb(106, 119, 92, 109, 121, 96), 3, { x, y, z -> LocationUtils.findP3Section(x, y, z) == 1 }),
        EE2_BOX(aabb(57, 108, 130, 59, 110, 132), 4, { x, y, z -> LocationUtils.findP3Section(x, y, z) == 2 }),
        HEE2_BOX(aabb(57, 132, 138, 62, 133, 140), 4, EE2_BOX.check),
        EE3_BOX(aabb(1, 108, 101, 3, 110, 107), 3, { x, y, z -> LocationUtils.findP3Section(x, y, z) == 3 }),
        CORE_BOX(aabb(51, 114, 54, 58, 117, 49), 4, { x, y, z -> LocationUtils.findP3Section(x, y, z) == 4 }),
        INCORE_BOX(CORE_BOX.box.move(.0, .0, 6.0), 4, { x, y, z -> aabb(68, 106, 54, 42, 155, 119).contains(x, y, z) }),
        RELIC_BOX(aabb(51.5, 3, 73.5, 57.5, 8, 79.5), 4, { _, y, _ -> LocationUtils.getPhase(y) == 5 });

        val maxCount get() = min(DungeonListener.dungeonTeammatesNoSelf.size, _maxCount)
        private val leapedIds = mutableSetOf<Int>()
        var completed = false
        val count get() = leapedIds.size

        fun updateCounter(id: Int, x: Double, y: Double, z: Double) {
            if (id in leapedIds) return
            if (! check(x, y, z)) return
            leapedIds.add(id)

            if (leapedIds.size == maxCount && alertComplete.value) {
                ChatUtils.showTitle(completeText.value)
                completeSound.action.invoke()
            }

            if (leapedIds.size >= maxCount) ThreadUtils.setTimeout(1000) {
                completed = true
            }
        }

        companion object {
            fun reset() = entries.forEach {
                it.leapedIds.clear()
                it.completed = false
            }
        }
    }
}