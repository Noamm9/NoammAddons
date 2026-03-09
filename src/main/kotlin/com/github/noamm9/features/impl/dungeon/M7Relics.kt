package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.config.PersonalBest
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.showIf
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.MathUtils.center
import com.github.noamm9.utils.MathUtils.toPos
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.PlayerUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.enums.WitherRelic
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import com.github.noamm9.utils.render.Render3D
import kotlinx.coroutines.launch
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.phys.Vec3
import java.awt.Color

object M7Relics: Feature(name = "M7 Relics", description = "A bunch of M7 Relics features") {
    private val relicBox by ToggleSetting("Box Relics").withDescription("Draws a box on where the relics are spawning and the cauldron you need to place.")
    private val relicSpawnTimer by ToggleSetting("Spawn Timer").withDescription("Shows on screen when the relic will spawn.")
    private val relicTimer by ToggleSetting("Place Timer").withDescription("Sends in chat the time it took to place the relic after you picked it up.")
    private val relicLook by ToggleSetting("Relic Look").withDescription("Automatically rotate to the relic cauldron after you pick it up.")
    private val relicLookTime by SliderSetting("Relic Look Time", 150L, 10, 300, 1).showIf { relicLook.value }.withDescription("How fast should the auto rotate (in milliseconds)")
    private val blockWrongRelic by ToggleSetting("Block Wrong Relic").withDescription("Prevents you from placing your relic at the wrong cauldron.")

    private val relicAura by ToggleSetting("Relic Aura").withDescription("Automatically pick up the relic when it spawns.")
    private var lastClick = System.currentTimeMillis()

    private val relicPickUpRegex = Regex("^(\\w{3,16}) picked the Corrupted (\\w{3,6}) Relic!$")
    private val relicTimes = mutableListOf<RelicEntry>()
    private var spawnTimerTicks: Long = 0
    private var p5StartTime = 0L

    private data class RelicEntry(
        val relic: WitherRelic,
        val player: String,
        val pickupTimeMs: Long,
        var placeTimeSeconds: Double = 0.0,
        var isPlaced: Boolean = false,
        var isPB: Boolean = false
    )

    override fun init() {
        register<WorldChangeEvent> {
            spawnTimerTicks = 0
            relicTimes.clear()
        }

        register<ChatMessageEvent> {
            if (! LocationUtils.inDungeon || ! LocationUtils.inBoss || LocationUtils.dungeonFloor != "M7") return@register
            val msg = event.unformattedText

            when {
                msg == "[BOSS] Necron: All this, for nothing..." -> {
                    p5StartTime = DungeonListener.currentTime
                    if (relicSpawnTimer.value) {
                        spawnTimerTicks = DungeonListener.currentTime + 42
                    }
                }

                relicTimer.value -> {
                    relicPickUpRegex.find(msg)?.destructured?.let { (player, relicType) ->
                        val relic = WitherRelic.fromName("Corrupted $relicType Relic") ?: return@register
                        relicTimes.add(RelicEntry(relic, player, System.currentTimeMillis()))
                    }
                }
            }
        }

        fun onInteract(event: PlayerInteractEvent, pos: BlockPos) {
            if (! blockWrongRelic.value || LocationUtils.F7Phase != 5) return
            val item = event.item?.hoverName?.string ?: return
            val relic = WitherRelic.fromName(item) ?: return
            if (pos.x == relic.cauldronPos.x.toInt() && pos.z == relic.cauldronPos.z.toInt()) return
            event.cancel()
        }

        register<PlayerInteractEvent.LEFT_CLICK.BLOCK> { onInteract(event, event.pos) }
        register<PlayerInteractEvent.RIGHT_CLICK.BLOCK> { onInteract(event, event.pos) }

        register<MainThreadPacketReceivedEvent.Post> {
            if (! relicLook.value || LocationUtils.F7Phase != 5) return@register
            if (event.packet !is ClientboundContainerSetSlotPacket) return@register
            val item = PlayerUtils.getHotbarSlot(8)?.hoverName ?: return@register
            val relic = WitherRelic.fromName(item.unformattedText) ?: return@register
            if (relic == WitherRelic.RED || relic == WitherRelic.ORANGE) scope.launch {
                PlayerUtils.rotateSmoothly(relic.cauldronPos.center(), relicLookTime.value)
            }
        }

        hudElement("Relic Spawn Timer", { relicSpawnTimer.value }, { (spawnTimerTicks - DungeonListener.currentTime) > 0 }, centered = true) { ctx, example ->
            val timeLeft = if (example) 25 else spawnTimerTicks - DungeonListener.currentTime
            val displayTime = (timeLeft / 20.0).toFixed(2)
            val color = DungeonListener.thePlayer?.clazz?.color ?: Color.WHITE
            Render2D.drawCenteredString(ctx, displayTime, 0, 0, color)
            return@hudElement displayTime.width().toFloat() to 9f
        }

        register<RenderWorldEvent> {
            if (! relicBox.value) return@register
            if (LocationUtils.F7Phase != 5) return@register
            val heldItem = mc.player?.inventory?.getItem(8)?.hoverName?.string ?: return@register
            WitherRelic.fromName(heldItem)?.let {
                Render3D.renderBlock(event.ctx, it.cauldronPos.toPos(), it.color, phase = true)
                Render3D.renderTracer(event.ctx, it.cauldronPos.add(0.5, 0.5, 0.5), it.color)
            }
        }

        register<TickEvent.Start> {
            if (! relicTimer.value || LocationUtils.F7Phase != 5 || relicTimes.isEmpty()) return@register
            val activeRelics = relicTimes.filter { ! it.isPlaced }.takeUnless { it.isEmpty() } ?: return@register

            val relicStands = mc.level !!.entitiesForRendering().filterIsInstance<ArmorStand>().filter {
                it.getItemBySlot(EquipmentSlot.HEAD).hoverName.string.contains("Relic")
            }

            for (entity in relicStands) for (entry in activeRelics) {
                if (! isEntityAtCauldron(entity.position(), entry.relic)) continue
                val currentTime = (DungeonListener.currentTime - p5StartTime) / 20.0
                entry.placeTimeSeconds = currentTime.toFixed(2).toDouble()
                entry.isPlaced = true

                if (entry.player != mc.user.name) continue
                entry.isPB = PersonalBest.checkAndSetPB(
                    key = "M7_relic_${entry.relic.name}",
                    value = entry.placeTimeSeconds,
                    lowerIsBetter = true
                )
            }

            if (relicTimes.size == 5 && relicTimes.all { it.isPlaced }) {
                relicTimes.sortedBy { it.placeTimeSeconds }.forEach { entry ->
                    val pbSuffix = if (entry.isPB) " §d§l(PB)" else ""
                    ChatUtils.modMessage("${entry.relic.coloredName} §aRelic placed in §e${entry.placeTimeSeconds}s§a.$pbSuffix")
                }
                relicTimes.clear()
            }
        }

        register<TickEvent.Start> {
            if (! relicAura.value) return@register
            if (LocationUtils.F7Phase != 5) return@register
            if (System.currentTimeMillis() - lastClick < 200) return@register
            if (mc.player !!.inventory.getItem(8).displayName.string.contains("Relic")) return@register

            val armorStand = mc.level?.entitiesForRendering()?.filterIsInstance<ArmorStand>()?.find {
                val isRelic = it.getItemBySlot(EquipmentSlot.HEAD).displayName.string.contains("Relic")
                val atCauldron = WitherRelic.entries.any { relic -> isEntityAtCauldron(it.position(), relic) }
                val distance = it.position().distanceTo(mc.player !!.position())
                isRelic && ! atCauldron && distance < 3
            } ?: return@register

            PlayerUtils.interactEntity(armorStand, InteractionHand.MAIN_HAND)
            PlayerUtils.swingArm()
            lastClick = System.currentTimeMillis()
        }
    }

    private fun isEntityAtCauldron(pos: Vec3, relic: WitherRelic): Boolean {
        val relicVec2 = Vec3(relic.coords.first.toDouble(), 0.0, relic.coords.second.toDouble())
        val entityVec2 = Vec3(pos.x, 0.0, pos.z)
        return entityVec2.distanceTo(relicVec2) < 1.5
    }
}