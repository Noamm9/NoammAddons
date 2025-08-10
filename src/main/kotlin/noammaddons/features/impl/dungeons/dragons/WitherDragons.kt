package noammaddons.features.impl.dungeons.dragons

import gg.essential.elementa.utils.withAlpha
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.network.play.server.*
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.features.impl.dungeons.dragons.DragonCheck.dragonSpawn
import noammaddons.features.impl.dungeons.dragons.DragonCheck.dragonSprayed
import noammaddons.features.impl.dungeons.dragons.DragonCheck.dragonUpdate
import noammaddons.features.impl.dungeons.dragons.DragonCheck.trackArrows
import noammaddons.features.impl.dungeons.dragons.WitherDragonEnum.*
import noammaddons.features.impl.dungeons.dragons.WitherDragonEnum.Companion.WitherDragonState.*
import noammaddons.features.impl.dungeons.dragons.WitherDragonEnum.Companion.handleSpawnPacket
import noammaddons.features.impl.esp.EspSettings
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.*
import noammaddons.utils.MathUtils.add
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderUtils.renderManager
import org.lwjgl.opengl.GL11
import java.awt.Color

object WitherDragons: Feature(
    name = "Wither Dragons",
    desc = "M7 dragons timers, boxes, priority, health and alerts."
) {
    private val s by SeperatorSetting("Dragon Timer")
    private val dragonTimer by ToggleSetting("Dragon Timer ", true)
    private val dragonTimerStyle by DropdownSetting("Timer Style", listOf("Milliseconds", "Seconds", "Ticks"), 0).addDependency { ! dragonTimer }
    private val showSymbol by ToggleSetting("Timer Symbol", true).addDependency { ! dragonTimer }

    private val ss by SeperatorSetting("Dragon Boxes")
    private val dragonBoxes by ToggleSetting("Dragon Boxes ", true)
    private val lineThickness by SliderSetting("Line Width", 1f, 5f, 0.1f, 2f).addDependency { ! dragonBoxes }

    private val sss by SeperatorSetting("Dragon Spawn ")
    private val dragonHealth by ToggleSetting("Dragon Health", true)
    private val highlightDragons by ToggleSetting("Highlight Dragons")
    val dragonTitle by ToggleSetting("Dragon Title", true)
    private val dragonTracers by ToggleSetting("Dragon Tracer", false)
    private val tracerThickness by SliderSetting("Tracer Width", 1f, 5f, 0.5f, 1f).addDependency { ! dragonTracers }

    private val ssss by SeperatorSetting("Dragon Alerts ")
    val sendTime by ToggleSetting("Send Dragon Time Alive", true)
    val sendSpray by ToggleSetting("Send Ice Sprayed", true)
    val sendArrowHit by ToggleSetting("Send Arrows Hit", true)

    private val sssss by SeperatorSetting("Dragon Priority ")
    val dragonPriorityToggle by ToggleSetting("Dragon Priority", false)
    val normalPower by SliderSetting("Normal Power", 0f, 32f, 1f, 22f).addDependency { ! dragonPriorityToggle }
    val easyPower by SliderSetting("Easy Power", 0f, 32f, 1f, 19f).addDependency { ! dragonPriorityToggle }
    val soloDebuff by DropdownSetting("Purple Solo Debuff", listOf("Tank", "Healer")).addDependency { ! dragonPriorityToggle }
    val soloDebuffOnAll by ToggleSetting("Solo Debuff on All Splits", true).addDependency { ! dragonPriorityToggle }

    var priorityDragon = None
    var currentTick: Long = 0

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) = WitherDragonEnum.reset()

    @SubscribeEvent
    fun onPacketReceived(event: PacketEvent.Received) {
        if (LocationUtils.F7Phase != 5) return
        when (val packet = event.packet) {
            is S2APacketParticles -> handleSpawnPacket(packet)
            is S04PacketEntityEquipment -> dragonSprayed(packet)
            is S0FPacketSpawnMob -> dragonSpawn(packet)
            is S1CPacketEntityMetadata -> dragonUpdate(packet)
            is S29PacketSoundEffect -> trackArrows(packet)
        }
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        WitherDragonEnum.entries.forEach { if (it.state == SPAWNING && it.timeToSpawn > 0) it.timeToSpawn -- }
        currentTick ++
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (LocationUtils.F7Phase != 5) return

        if (dragonHealth) DragonCheck.dragonEntityList.filter { it.health > 0 }.forEach {
            RenderUtils.drawString(formatHealth(it.health), it.renderVec.add(y = - 1), Color.WHITE, 6f, true)
        }

        WitherDragonEnum.entries.forEach { dragon ->
            if (dragonTimer && dragon.state == SPAWNING && dragon.timeToSpawn > 0) RenderUtils.drawString(
                "&${dragon.colorCode}${dragon.name}: ${getDragonTimer(dragon.timeToSpawn)}",
                dragon.spawnPos, Color.WHITE, 6f, false
            )

            if (dragonBoxes && dragon.state != DEAD) drawDragonBox(
                dragon.boxesDimensions, dragon.color.withAlpha(0.5f), lineThickness
            )
        }

        if (priorityDragon != None && dragonTracers && priorityDragon.state == SPAWNING) {
            RenderUtils.drawTracer(priorityDragon.spawnPos.add(0.5, 3.5, 0.5), color = priorityDragon.color, lineWidth = tracerThickness)
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! dragonTimer) return
        priorityDragon.takeIf { it != None }?.let { dragon ->
            if (dragon.state != SPAWNING || dragon.timeToSpawn <= 0) return
            RenderUtils.drawCenteredText(
                "&${dragon.colorCode}${getDragonTimer(dragon.timeToSpawn)}",
                mc.getWidth() / 2f,
                mc.getHeight() * 0.4f,
                3f,
            )
        }
    }

    @SubscribeEvent
    @Suppress("UNCHECKED_CAST")
    fun onRenderModelEvent(event: PostRenderEntityModelEvent) {
        if (! highlightDragons) return
        if (LocationUtils.F7Phase != 5) return
        if (WitherDragonEnum.entries.none { it.entity != null }) return

        val phaseSetting = EspSettings.getSettingByName("Phase") as? ToggleSetting ?: return
        val lineSetting = EspSettings.getSettingByName("Line Width") as? Component<Number> ?: return

        val originalPhase = phaseSetting.value
        val originalLineWidth = lineSetting.value

        phaseSetting.value = false
        lineSetting.value = 20f

        WitherDragonEnum.entries
            .asSequence()
            .filter { it.state == ALIVE }
            .mapNotNull { it.entity?.let { entity -> it to entity } }
            .forEach { (dragon, entity) ->
                EspUtils.espMob(entity, dragon.color, EspUtils.ESPType.FILLED_OUTLINE.ordinal)
            }

        phaseSetting.value = originalPhase
        lineSetting.value = originalLineWidth
    }


    private fun getDragonTimer(spawnTime: Int): String = when (dragonTimerStyle) {
        0 -> "${(spawnTime * 50)}${if (showSymbol) "ms" else ""}"
        1 -> "${(spawnTime / 20f).toFixed(1)}${if (showSymbol) "s" else ""}"
        else -> "${spawnTime}${if (showSymbol) "t" else ""}"
    }

    private fun formatHealth(health: Float): String {
        val color = when {
            health >= 750_000_000 -> "&a"
            health >= 500_000_000 -> "&e"
            health >= 250_000_000 -> "&6"
            else -> "&c"
        }

        val str = when {
            health >= 1_000_000_000 -> {
                val b = health / 1_000_000_000
                "${if (b > 1) b.toFixed(1) else b.toInt()}b"
            }

            health >= 1_000_000 -> "${(health / 1_000_000).toInt()}m"
            health >= 1_000 -> "${(health / 1_000).toInt()}k"
            else -> "${health.toInt()}"
        }

        return color + str
    }

    private fun drawDragonBox(aabb: AxisAlignedBB, color: Color, outlineWidth: Number = 3) {
        GlStateManager.pushMatrix()
        RenderUtils.preDraw()
        GL11.glLineWidth(outlineWidth.toFloat())
        RenderUtils.drawOutlinedAABB(aabb.offset(- renderManager.viewerPosX, - renderManager.viewerPosY, - renderManager.viewerPosZ), color)
        GL11.glLineWidth(outlineWidth.toFloat())
        RenderUtils.postDraw()
        GlStateManager.popMatrix()
    }
}
