package noammaddons.features.dungeons

import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.boss.BossStatus
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityGhast
import net.minecraft.init.Blocks.*
import net.minecraft.item.ItemBow
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.EspUtils.EspMob
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.isMasterMode
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderHelper
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.drawTracer
import noammaddons.utils.SoundUtils
import java.awt.Color
import kotlin.math.abs

object Floor4BossFight: Feature() {
    private val inM4boss get() = dungeonFloorNumber == 4 && inBoss
    private const val bearSpawnTime = 68
    private var bearSpawning = false
    private var timer = - 1

    private val hitShot = listOf(
        Regex("\\[BOSS] Thorn: I feel...dizzy..."),
        Regex("\\[BOSS] Thorn: It hurts...what a delicate feeling..."),
        Regex("\\[BOSS] Thorn: Round and round, another wound."),
        Regex("\\[BOSS] Thorn: My energy, it goes away...")
    )

    private val missedShot = listOf(
        Regex("\\[CROWD] .+: .+ missed the shot! No way!! Hahaha"),
        Regex("\\[CROWD] .+: My goodness, .+ really can't aim!!"),
        Regex("\\[CROWD] .+: Alright those humans are a joke, missing easy shots like that..."),
        Regex("\\[CROWD] .+: Yeah!!! Keep dodging them Thorn!"),
        Regex("\\[CROWD] .+: .+ has no thumbs!")
    )

    @SubscribeEvent
    fun onPostRenderEntityModel(event: PostRenderEntityModelEvent) {
        if (! inM4boss) return
        when (val entity = event.entity) {
            is EntityGhast -> if (config.espThorn) EspMob(event, config.espThornColor, config.espOutlineWidth * 2)
            is EntityOtherPlayerMP -> {
                if (! config.espSpiritBear) return
                if (! entity.displayName.noFormatText.lowercase().startsWith("spirit bear")) return
                EspMob(event, config.espSpiritBearColor)
            }
        }
    }

    @SubscribeEvent
    fun onRenderEntity(event: RenderEntityEvent) {
        if (! inM4boss) return
        if (! config.traceSpiritBow) return
        val entity = event.entity as? EntityArmorStand ?: return
        if (! entity.isInvisible) return
        if (entity.heldItem?.item !is ItemBow) return
        drawTracer(entity.renderVec.add(Vec3(.0, 1.0, .0)), config.traceSpiritBowColor)
    }

    @SubscribeEvent
    fun onBlock(event: BlockChangeEvent) {
        if (! config.spiritBearSpawnTimer) return
        if (! inM4boss) return
        if (event.pos != BlockPos(7, 77, 34)) return
        if (event.block != sea_lantern) return
        if (event.oldBlock != coal_block) return

        bearSpawning = true
        timer = bearSpawnTime
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        if (! inM4boss) return
        if (! bearSpawning) return
        timer -= 1
        bearSpawning = timer >= 0
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! inM4boss) return
        if (! bearSpawning) return

        drawCenteredText(
            "${timer / 20.0}".toFixed(2),
            mc.getWidth() / 2f,
            mc.getHeight() * 0.25f,
            3f, Color(255, 0, 255)
        )
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! config.spiritBowHitMissAlert) return
        if (! inM4boss) return
        val msg = event.component.noFormatText

        hitShot.forEach { regex ->
            if (! msg.matches(regex)) return@forEach
            showTitle("&l&dHIT")
            SoundUtils.Pling()
        }

        missedShot.forEach { regex ->
            if (! msg.matches(regex)) return@forEach
            showTitle("&l&cMISS")
            SoundUtils.harpNote()
        }
    }


    @SubscribeEvent
    fun onBossbarUpdateEvent(event: BossbarUpdateEvent.Post) {
        if (! inM4boss || event.bossName != "§c§lThorn§r") return
        val hpList = if (isMasterMode) listOf(100, 83, 66, 48, 31, 13, 0)
        else listOf(100, 74, 47, 21, 0)
        val rightSide = hpList.lastIndex
        val leftSide = rightSide - (hpList.withIndex().minByOrNull { (_, value) -> abs(value - event.healthPresent.toInt()) }?.index ?: 0)
        val str = RenderHelper.colorCodeByPresent(leftSide, rightSide) + "$leftSide/$rightSide"
        BossStatus.bossName = "${event.bossName}  $str"
    }
}