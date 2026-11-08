package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.NoammAddons.isCheat
import com.github.noamm9.compat.Enderman
import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.LocationUtils.inBoss
import com.github.noamm9.utils.render.world.Render3D.renderBoxBounds
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.arrow.AbstractArrow
import net.minecraft.world.phys.AABB
import java.awt.Color

object StarMobESP: Feature(
    "Highlights all starred mobs in a dungeon.",
    //#if LEGIT
    //!name = "Star Mob Highlight",
    //#endif
    jsonName = "Star Mob ESP"
) {
    private val espBats by ToggleSetting("Highlight Bats", true).withDescription("Highlights Bats in Dungeons.")

    private val starMobColor by ColorSetting("Star Mob Color", Color.YELLOW, false).section("General Colors").withDescription("Default color for all Starred mobs.")
    private val felColor by ColorSetting("Fel Color", Color.PINK, false).withDescription("The color used for fels.")
    private val batColor by ColorSetting("Bat Color", Color.GREEN, false).withDescription("The color used for highlighted bats.").showIf { espBats.value }
    private val mode by DropdownSetting("Mode", 0, listOf("Outline", "Fill", "Filled Outline"))
    private val lineWidth by SliderSetting("Line Width", 2.5, 1, 10, 0.1).hideIf { mode.value == 1 }
    private val phase by ToggleSetting("Phase", false).showIf { isCheat }

    private val batHealths = setOf(100f, 200f, 400f, 800f)
    private val bats = HashSet<Int>()
    private val starMobs = HashSet<Int>()
    private val checked = HashSet<Int>()

    override fun init() {
        register<MainThreadPacketReceivedEvent.Post> {
            if (! LocationUtils.inDungeon || inBoss) return@register
            if (event.packet !is ClientboundSetEntityDataPacket) return@register
            val entity = level.getEntity(event.packet.id) ?: return@register
            if (entity is ArmorStand) {
                val name = entity.name.string
                if (name.endsWith("❤") && name.contains("✯")) {
                    checkStarMob(entity, name)
                }
            }
            if (entity !is Bat) return@register
            if (! espBats.value) return@register
            if (entity.maxHealth !in batHealths) return@register
            bats.add(entity.id)
        }

        register<EntityUnloadEvent> {
            if (! LocationUtils.inDungeon || inBoss) return@register
            bats.remove(event.entity.id)
            starMobs.remove(event.entity.id)
            checked.remove(event.entity.id)
        }

        register<WorldChangeEvent> {
            bats.clear()
            starMobs.clear()
            checked.clear()
        }

        register<RenderWorldEvent> {
            if (! LocationUtils.inDungeon || inBoss) return@register
            val outline = mode.value.equalsOneOf(0, 2)
            val fill = mode.value.equalsOneOf(1, 2)
            val partialTick = mc.deltaTracker.getGameTimeDeltaPartialTick(false)
            starMobs.forEach { id ->
                val entity = level.getEntity(id) ?: return@forEach
                val color = if (entity is Enderman) felColor.value
                else starMobColor.value
                event.ctx.renderBoxBounds(
                    entity.getInterpolatedBoundingBox(partialTick),
                    color,
                    color.withAlpha(0.2f),
                    outline,
                    fill,
                    phase.value,
                    lineWidth.value
                )
            }
            bats.forEach { id ->
                val entity = level.getEntity(id) ?: return@forEach
                event.ctx.renderBoxBounds(
                    entity.getInterpolatedBoundingBox(partialTick),
                    batColor.value,
                    batColor.value.withAlpha(0.2f),
                    outline,
                    fill,
                    phase.value,
                    lineWidth.value
                )
            }
        }
    }

    private fun checkStarMob(armorStand: Entity, name: String) {
        if (! checked.add(armorStand.id)) return
        val name = name.removeFormatting().uppercase()
        // withermancers are always -3 to real entity the -1 and -2 are the wither skulls that they shoot
        val offset = if (name.contains("WITHERMANCER")) 3 else 1
        val id = armorStand.id - offset

        val mob = armorStand.level().getEntity(id)
        if (mob !is ArmorStand && id !in starMobs && mob != null) {
            starMobs.add(id)
            return
        }

        val possibleEntities = armorStand.level().getEntities(
            armorStand, armorStand.boundingBox.move(0.0, - 1.0, 0.0)
        ) { it !is ArmorStand && it !is ExperienceOrb }

        possibleEntities.find {
            ! starMobs.contains(it.id) && when (it) {
                is Player -> ! it.isInvisible && it.uuid.version() == 2 && it != player
                is WitherBoss -> false
                is AbstractArrow -> false
                else -> true
            }
        }?.let {
            starMobs.add(it.id)
        }
    }
    // since entity.boundingBox is only updated 20 times per second
    // we use getInterpolatedBoundingBox instead to make the box render smoothly on moving entities
    //? if <26.3 {
    private fun Entity.getInterpolatedBoundingBox(partialTicks: Float) : AABB {
        // 26.2 does not have this function by default
        val interpX = xOld + (x - xOld) * partialTicks
        val interpY = yOld + (y - yOld) * partialTicks
        val interpZ = zOld + (z - zOld) * partialTicks

        return boundingBox.move(
            interpX - x,
            interpY - y,
            interpZ - z
        )
    }
    //? }
}