package com.github.noamm9.features.impl.visual

import com.github.noamm9.event.impl.BlockChangeEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.ColorUtils
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.chunk.status.ChunkStatus
import java.awt.Color
import kotlin.math.max
import kotlin.math.roundToInt

object LavaToWater : Feature("Replaces lava with the water texture and water fog (resource-pack aware).") {
    val colorTint by ToggleSetting("Color Tint", false).onChange { refreshTrackedSections() }
    val tintColor by ColorSetting("Tint Color", Color(0x3F, 0x76, 0xE4), false)
        .showIf { colorTint.value && !animatedTint.value }
        .onChange { refreshTrackedSections() }

    val animatedTint by ToggleSetting("Animated Tint", false)
        .withDescription("Cycles the tint through the rainbow instead of a fixed color. Only rebuilds the sections that contain lava (not the whole world), and keeps them all in sync so the color stays uniform.")
        .showIf { colorTint.value }
        .onChange { refreshTrackedSections() }

    val animationSpeed by SliderSetting("Animation Speed", 1f, 0.1f, 5f, 0.1f, "x")
        .withDescription("How fast the color cycles. Also controls how often lava sections are re-synced - higher speeds mean more frequent (and slightly heavier) updates.")
        .showIf { colorTint.value && animatedTint.value }
    
    private val lavaSectionSet = HashSet<Long>()
    private val lavaSectionOrder = ArrayDeque<Long>()

    private const val RESCAN_INTERVAL_TICKS = 60
    private var rescanCounter = 0
    private var tickCounter = 0

    private const val EXTRA_CHUNK_RADIUS = 1

    private const val BASE_CYCLE_MS = 8000L // full rainbow loop at 1x speed

    val currentTintColor: Color
        get() {
            if (!animatedTint.value) return tintColor.value
            val cycleDuration = (BASE_CYCLE_MS / animationSpeed.value).toLong().coerceAtLeast(200L)
            return ColorUtils.rainbow(cycleDuration)
        }

    override fun init() {
        register<BlockChangeEvent> {
            if (!enabled) return@register
            if (event.newBlock == Blocks.LAVA || event.oldBlock == Blocks.LAVA) {
                markSectionDirty(event.pos)
                mc.levelRenderer.setSectionDirty(
                    SectionPos.blockToSectionCoord(event.pos.x),
                    SectionPos.blockToSectionCoord(event.pos.y),
                    SectionPos.blockToSectionCoord(event.pos.z)
                )
            }
        }

        register<TickEvent.Start> {
            if (!enabled) {
                rescanCounter = 0
                tickCounter = 0
                return@register
            }

            if (colorTint.value && animatedTint.value) {
                val intervalTicks = max(2, (20f / (2f * animationSpeed.value)).roundToInt())
                tickCounter++
                if (tickCounter >= intervalTicks) {
                    tickCounter = 0
                    refreshAllTrackedSections()
                }
            }

            rescanCounter++
            if (rescanCounter >= RESCAN_INTERVAL_TICKS) {
                rescanCounter = 0
                rescanLavaSections()
            }
        }
    }

    private fun markSectionDirty(pos: BlockPos) {
        val packed = SectionPos.asLong(
            SectionPos.blockToSectionCoord(pos.x),
            SectionPos.blockToSectionCoord(pos.y),
            SectionPos.blockToSectionCoord(pos.z)
        )
        if (lavaSectionSet.add(packed)) lavaSectionOrder.addLast(packed)
    }

    private fun refreshAllTrackedSections() {
        if (mc.level == null || lavaSectionOrder.isEmpty()) return
        for (packed in lavaSectionOrder) {
            val section = SectionPos.of(packed)
            mc.levelRenderer.setSectionDirty(section.x(), section.y(), section.z())
        }
    }
    
    private fun findLavaSections(): List<Long> {
        val level = mc.level ?: return emptyList()
        val player = mc.player ?: return emptyList()
        val radius = mc.options.renderDistance().get() + EXTRA_CHUNK_RADIUS
        val pcx = player.chunkPosition().x
        val pcz = player.chunkPosition().z

        val found = ArrayList<Long>()
        for (cx in (pcx - radius)..(pcx + radius)) {
            for (cz in (pcz - radius)..(pcz + radius)) {
                val chunk = level.getChunk(cx, cz, ChunkStatus.FULL, false) ?: continue
                val sections = chunk.sections
                for (i in sections.indices) {
                    val section = sections[i] ?: continue
                    if (section.hasOnlyAir()) continue
                    if (section.states.maybeHas { it.block === Blocks.LAVA }) {
                        found.add(SectionPos.asLong(cx, chunk.getSectionYFromSectionIndex(i), cz))
                    }
                }
            }
        }
        return found
    }

    private fun rescanLavaSections() {
        if (mc.level == null) return
        lavaSectionSet.clear()
        lavaSectionOrder.clear()
        for (packed in findLavaSections()) {
            if (lavaSectionSet.add(packed)) lavaSectionOrder.addLast(packed)
        }
    }

    private fun refreshTrackedSections() {
        if (!enabled || mc.level == null) return
        rescanLavaSections()
        refreshAllTrackedSections()
    }

    override fun onEnable() {
        super.onEnable()
        rescanCounter = 0
        tickCounter = 0
        refreshTrackedSections()
    }

    override fun onDisable() {
        super.onDisable()
        rescanCounter = 0
        tickCounter = 0
        refreshAllTrackedSections()
        lavaSectionSet.clear()
        lavaSectionOrder.clear()
    }
}
