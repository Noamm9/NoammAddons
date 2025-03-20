package noammaddons.features.dungeons.solvers

import gg.essential.elementa.utils.withAlpha
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.utils.BlockUtils.getMetadata
import noammaddons.utils.BlockUtils.getStateAt
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.DungeonUtils.dungeonTeammates
import noammaddons.utils.EspUtils.EspMob
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.MathUtils.add
import noammaddons.utils.NumbersUtils.format
import noammaddons.utils.RenderHelper.getRainbowColor
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderUtils.drawEntityBox
import noammaddons.utils.RenderUtils.drawString
import noammaddons.utils.RenderUtils.drawTracer
import noammaddons.utils.SoundUtils
import noammaddons.utils.Utils.equalsOneOf


object LividSolver: Feature() {
    private const val LIVID_BOSS_START_MSG = "[BOSS] Livid: Welcome, you've arrived right on time. I am Livid, the Master of Shadows."
    private var currentLivid: EntityOtherPlayerMP? = null
    private var lividStart = false
    private var bossTicks = 390
    private val color get() = getRainbowColor(1f)
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

    @SubscribeEvent
    fun onEvent(event: Event) {
        if (! config.lividFinder) return
        if (dungeonFloorNumber != 5) return
        if (! inBoss) return
        if (currentLivid == null) return
        if (currentLivid?.isDead.equalsOneOf(null, true)) return
        if (currentLivid?.isPlayerSleeping.equalsOneOf(null, true)) return

        when (event) {
            is PostRenderEntityModelEvent -> {
                if (! config.espType.equalsOneOf(0, 2)) return
                if (event.entity != currentLivid) return

                EspMob(event, color)
            }

            is RenderEntityEvent -> {
                if (! config.hideWrongLivids) return
                if (event.entity == currentLivid) return
                if (dungeonTeammates.map { it.entity?.entityId }.contains(event.entity.entityId)) return
                if (event.entity is EntityArmorStand && ! event.entity.name.contains("Livid")) return

                event.isCanceled = true
            }

            is RenderWorld -> {
                if (config.espType == 1) drawEntityBox(currentLivid !!, color.withAlpha(77))
                drawTracer(currentLivid !!.renderVec.add(y = 0.9), color)

                drawString(
                    format(currentLivid !!.health),
                    currentLivid !!.renderVec.add(y = 3.0),
                    color, 1.5f
                )
            }
        }
    }


    @SubscribeEvent
    fun onTick(event: Tick) {
        if (! inBoss || dungeonFloorNumber != 5) {
            currentLivid = null
            return
        }

        val metadata = getStateAt(BlockPos(5, 109, 42)).getMetadata()
        val lividData = lividNames.entries.find { it.key == metadata } ?: return
        val lividEntity = mc.theWorld?.loadedEntityList?.find { it.name == lividData.value } ?: return

        currentLivid = lividEntity as? EntityOtherPlayerMP ?: return
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldUnloadEvent) {
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
        if (! config.lividFinder) return
        if (dungeonFloorNumber != 5) return
        if (event.component.noFormatText != LIVID_BOSS_START_MSG) return
        lividStart = true
    }
}