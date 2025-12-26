package noammaddons.features.impl.dungeons

import gg.essential.elementa.utils.withAlpha
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.boss.BossStatus
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityGhast
import net.minecraft.entity.passive.*
import net.minecraft.init.Blocks.*
import net.minecraft.item.ItemBow
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.EspUtils.ESPType.*
import noammaddons.utils.EspUtils.espMob
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.isMasterMode
import noammaddons.utils.MathUtils.add
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderHelper
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.drawTracer
import java.awt.Color
import kotlin.math.abs

object Floor4BossFight: Feature(name = "Floor 4 Boss", desc = "Spirit bear spawn timer, hightlights, and more") {
    private val inM4boss get() = dungeonFloorNumber == 4 && inBoss
    private const val bearSpawnTime = 68
    private var bearSpawning = false
    private var timer = - 1

    private val espThorn by ToggleSetting("ESP Thorn")
    private val espSpiritBear by ToggleSetting("ESP Spirit Bear")
    private val spiritBearSpawnTimer by ToggleSetting("Bear Spawn Timer")
    private val boxSpiritBow by ToggleSetting("Box Spirit Bow")
    private val traceSpiritBow by ToggleSetting("Trace Spirit Bow")
    private val spiritMobEsp by MultiCheckboxSetting(
        "ESP Spirit Mobs", mapOf(
            "Rabbit" to false, "Wolf" to false,
            "Sheep" to false, "Cow" to false,
            "Chicken" to false,
        )
    )
    private val aaa by SeperatorSetting("Colors")
    private val boxSpiritBowColor by ColorSetting("Box Color", Color.CYAN.withAlpha(50))
    private val traceSpiritBowColor by ColorSetting("Tracer Color", Color.CYAN, false)
    private val espThornColor by ColorSetting("Thorn Color", Color.RED.withAlpha(50))
    private val espSpiritBearColor by ColorSetting("Bear Color", Color(255, 0, 255).withAlpha(50))


    @SubscribeEvent
    fun onPostRenderEntityModel(event: PostRenderEntityModelEvent) {
        if (! inM4boss) return
        when (val entity = event.entity) {
            is EntityGhast -> if (espThorn) espMob(event.entity, espThornColor)
            is EntityOtherPlayerMP -> {
                if (! espSpiritBear) return
                if (! entity.displayName.noFormatText.lowercase().startsWith("spirit bear")) return
                espMob(event.entity, espSpiritBearColor)
            }

            is EntitySheep -> {
                if (! spiritMobEsp["Sheep"] !!) return
                espMob(event.entity, Color.CYAN.withAlpha(50))
            }

            is EntityRabbit -> {
                if (! spiritMobEsp["Rabbit"] !!) return
                espMob(event.entity, Color.CYAN.withAlpha(50))
            }

            is EntityWolf -> {
                if (! spiritMobEsp["Wolf"] !!) return
                espMob(event.entity, Color.CYAN.withAlpha(50))
            }

            is EntityCow -> {
                if (! spiritMobEsp["Cow"] !!) return
                espMob(event.entity, Color.CYAN.withAlpha(50))
            }

            is EntityChicken -> {
                if (! spiritMobEsp["Chicken"] !!) return
                espMob(event.entity, Color.CYAN.withAlpha(50))
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (! inM4boss) return
        for (entity in mc.theWorld.loadedEntityList.filterIsInstance<EntityArmorStand>()) {
            if (! entity.isInvisible) continue
            if (entity.heldItem?.item !is ItemBow) continue
            if (traceSpiritBow) drawTracer(entity.renderVec.add(y = 1.0), traceSpiritBowColor)
            if (boxSpiritBow) espMob(entity, boxSpiritBowColor, BOX.ordinal)
        }
    }

    @SubscribeEvent
    fun onBlock(event: BlockChangeEvent) {
        if (! spiritBearSpawnTimer || ! inM4boss) return
        if (event.pos != BlockPos(7, 77, 34)) return
        if (event.oldBlock != coal_block) return
        if (event.block != sea_lantern) return

        bearSpawning = true
        timer = bearSpawnTime
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
    fun onServerTick(event: ServerTick) {
        if (! inM4boss || ! bearSpawning) return
        timer -= 1
        bearSpawning = timer >= 0
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
