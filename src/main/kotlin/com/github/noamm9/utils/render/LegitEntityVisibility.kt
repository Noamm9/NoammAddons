package com.github.noamm9.utils.render

import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.EntityUnloadEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Shortcuts
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.utils.MathUtils.getCorners
import com.github.noamm9.utils.render.RenderHelper.renderBoundingBox
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.util.Util
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import kotlin.math.floor

object LegitEntityVisibility: ISelfInit, Shortcuts {
    private const val MAX_DISTANCE = 128.0 * 128.0
    private const val MAX_ENTRIES = 1024
    private const val TTL_MILLIS = 100L

    private data class Entry(var visible: Boolean, var lastCheck: Long)

    private val cache = HashMap<Int, Entry>(256)

    override fun init() {
        EventBus.register<WorldChangeEvent> { cache.clear() }
        EventBus.register<EntityUnloadEvent> { cache.remove(event.entity.id) }
    }

    @JvmStatic
    @Suppress("unused")
    fun isVisible(player: LocalPlayer, entity: Entity): Boolean {
        if (entity === player) return true
        if (entity.isRemoved || ! entity.isAlive) {
            cache.remove(entity.id)
            return false
        }
        if (player.distanceToSqr(entity) > MAX_DISTANCE) {
            cache.remove(entity.id)
            return false
        }

        val now = Util.getMillis()
        val cached = cache[entity.id]
        if (cached != null && now - cached.lastCheck < TTL_MILLIS) return cached.visible

        val visible = ! entity.isInvisibleTo(player) && hasLineOfSight(entity)
        if (cached != null) {
            cached.visible = visible
            cached.lastCheck = now
        }
        else {
            if (cache.size >= MAX_ENTRIES) cache.clear()
            cache[entity.id] = Entry(visible, now)
        }

        return visible
    }
    private fun hasLineOfSight(target: Entity): Boolean {
        val source = mc.gameRenderer.mainCamera().position()
        target.renderBoundingBox.getCorners().forEach { corner ->
            if(hasClearLineOfSight(source, corner)) return true
        }
        return false
    }
    private fun hasClearLineOfSight(source: Vec3, target: Vec3): Boolean {
        val direction = target.subtract(source)
        val distance = source.distanceToSqr(target)

        val stepCount = (distance * 4).toInt().coerceAtLeast(1)
        val stepVector = direction.scale(1.0 / stepCount)

        var currentPos = source
        var lastCheckedBlockPos: BlockPos? = null

        repeat(stepCount) {
            val blockPos = BlockPos(
                floor(currentPos.x).toInt(),
                floor(currentPos.y).toInt(),
                floor(currentPos.z).toInt()
            )

            if (blockPos != lastCheckedBlockPos) {
                lastCheckedBlockPos = blockPos

                val state = level.getBlockState(blockPos)
                val shape = if (state.canOcclude()) state.getCollisionShape(level, blockPos) else null

                if (shape != null && !shape.isEmpty && shape.clip(source, target, blockPos) != null)
                    return false
            }
            currentPos = currentPos.add(stepVector)
        }
        return true
    }

}