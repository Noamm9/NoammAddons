package noammaddons.features.impl.dungeons.solvers

import gg.essential.elementa.utils.withAlpha
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.BlockUtils.getMetadata
import noammaddons.utils.BlockUtils.getStateAt
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.DungeonUtils.dungeonTeammates
import noammaddons.utils.EspUtils.espMob
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.MathUtils.add
import noammaddons.utils.NumbersUtils.format
import noammaddons.utils.RenderHelper
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderUtils.drawString
import noammaddons.utils.RenderUtils.drawTracer
import noammaddons.utils.SoundUtils
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.favoriteColor


object LividSolver: Feature() {
    private const val LIVID_BOSS_START_MSG = "[BOSS] Livid: Welcome, you've arrived right on time. I am Livid, the Master of Shadows."
    private var currentLivid: EntityOtherPlayerMP? = null
    private val ceilingWoolBlock = BlockPos(5, 109, 42)
    private var lividStart = false
    private var bossTicks = 390
    val lividNames = mapOf(
        13 to "Frog Livid",
        10 to "Purple Livid",
        7 to "Doctor Livid",
        11 to "Scream Livid",
        5 to "Smile Livid",
        14 to "Hockey Livid",
        2 to "Crossed Livid",
        4 to "Arcade Livid",
        0 to "Vendetta Livid"
    )

    private val showHp = ToggleSetting("Show HP", true)
    private val highlight = ToggleSetting("Highlight", true)
    private val tracer = ToggleSetting("Tracer", true)
    private val hideWrong = ToggleSetting("Hide Wrong")
    private val iceSprayTimer = ToggleSetting("Ice Spray Timer")
    private val highlightColor = ColorSetting("Highlight Color", favoriteColor.withAlpha(0.3f), false)
    private val tracerColor = ColorSetting("Tracer Color", favoriteColor, false)

    override fun init() = addSettings(
        showHp, highlight, tracer,
        hideWrong, iceSprayTimer,
        SeperatorSetting("Colors"),
        highlightColor, tracerColor
    )

    @SubscribeEvent
    fun onRenderEntity(event: RenderEntityEvent) {
        if (! hideWrong.value) return
        if (dungeonFloorNumber != 5) return
        if (! inBoss) return
        if (currentLivid == event.entity) return
        if (currentLivid?.isDead.equalsOneOf(null, true)) return
        if (currentLivid?.isPlayerSleeping.equalsOneOf(null, true)) return
        if (dungeonTeammates.map { it.entity?.entityId }.contains(event.entity.entityId)) return
        if (event.entity is EntityArmorStand && ! event.entity.name.contains("Livid")) return
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (dungeonFloorNumber != 5) return
        if (! inBoss) return
        if (currentLivid == null) return
        if (currentLivid?.isDead.equalsOneOf(null, true)) return
        if (currentLivid?.isPlayerSleeping.equalsOneOf(null, true)) return
        if (highlight.value) espMob(currentLivid !!, highlightColor.value)
        if (tracer.value) drawTracer(currentLivid !!.renderVec.add(y = 0.9), tracerColor.value)
        if (showHp.value) drawString(
            format(currentLivid !!.health),
            currentLivid !!.renderVec.add(y = 3.0),
            RenderHelper.colorByPresent(currentLivid !!.health, currentLivid !!.maxHealth), 1.5f
        )
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderEntityEvent) {
        if (dungeonFloorNumber != 5 || ! inBoss) return
        if (currentLivid?.isDead.equalsOneOf(null, true)) return
        if (currentLivid?.isPlayerSleeping.equalsOneOf(null, true)) return
        if (! highlight.value) return
        espMob(currentLivid !!, highlightColor.value)
    }

    @SubscribeEvent
    fun onTick(event: Tick) {
        if (! inBoss || dungeonFloorNumber != 5) {
            currentLivid = null
            return
        }

        val metadata = getStateAt(ceilingWoolBlock).getMetadata()
        val lividData = lividNames.entries.find { it.key == metadata } ?: return
        val lividEntity = mc.theWorld?.loadedEntityList?.find { it.name == lividData.value } ?: return
        currentLivid = lividEntity as? EntityOtherPlayerMP ?: return
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        currentLivid = null
        lividStart = false
        bossTicks = 390
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        if (! lividStart) return
        bossTicks --
        if (bossTicks == 0) {
            lividStart = false
            bossTicks = 390
            showTitle("&bIce Spray Livid!")
            SoundUtils.Pling()
        }
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! iceSprayTimer.value) return
        if (dungeonFloorNumber != 5) return
        if (event.component.noFormatText != LIVID_BOSS_START_MSG) return
        lividStart = true
    }
}