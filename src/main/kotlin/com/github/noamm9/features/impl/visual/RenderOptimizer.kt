package com.github.noamm9.features.impl.visual

import com.github.noamm9.event.impl.EntityCheckRenderEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.items.ItemUtils
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import java.util.*

object RenderOptimizer: Feature("Optimize Rendering by hiding useless shit.") {
    private val hideStar by ToggleSetting("Hide Star Mobs's Nametag")
    private val hideNonStar by ToggleSetting("Hide Non Star Mob's Nametag")
    private val hideFallingBlocks by ToggleSetting("Hide Falling Blocks")
    private val hideLightning by ToggleSetting("Hide Lightning Bolts")
    private val hideSoulWeaver by ToggleSetting("Hide Soul Weaver")
    private val hide0HealthNames by ToggleSetting("Hide 0 Health")
    private val hideDeadMobs by ToggleSetting("Hide Dead Mobs")
    private val hideXpOrbs by ToggleSetting("Hide Xp Orbs")
    private val removeTentacles by ToggleSetting("Hide P5 Tentacles")
    private val hideP5p by ToggleSetting("Hide P5 Particles")
    val hideFireOnEntities by ToggleSetting("Hide Fire On Entities")

    private val dungeonMobRegex = Regex("^(?:§.)*(.+).+§c❤$")
    private val healthMatches = arrayOf(
        Regex("^§.\\[§.Lv\\d+§.] §.+ (?:§.)+0§f/.+§c❤$"),
        Regex("^.+ (?:§.)+0§c❤$")
    )

    private const val TENTACLE_TEXTURE =
        "ewogICJ0aW1lc3RhbXAiIDogMTcxOTg1NzI3NzI0OSwKICAicHJvZmlsZUlkIiA6ICIxODA1Y2E2MmM0ZDI0M2NiOWQxYmY4YmM5N2E1YjgyNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJSdWxsZWQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzdkODM2NzQ5MjZiODk3MTRlNmI1YTU1NDcwNTAxYzA0YjA2NmRkODdiZjZjMzM1Y2RkYzZlNjBhMWExYTVmNSIKICAgIH0KICB9Cn0="
    private const val SOUL_WEAVER_TEXTURE =
        "eyJ0aW1lc3RhbXAiOjE1NTk1ODAzNjI1NTMsInByb2ZpbGVJZCI6ImU3NmYwZDlhZjc4MjQyYzM5NDY2ZDY3MjE3MzBmNDUzIiwicHJvZmlsZU5hbWUiOiJLbGxscmFoIiwic2lnbmF0dXJlUmVxdWlyZWQiOnRydWUsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yZjI0ZWQ2ODc1MzA0ZmE0YTFmMGM3ODViMmNiNmE2YTcyNTYzZTlmM2UyNGVhNTVlMTgxNzg0NTIxMTlhYTY2In19fQ=="

    override fun init() {
        register<MainThreadPacketReceivedEvent.Pre> {
            if (! LocationUtils.inSkyblock) return@register
            when (val packet = event.packet) {
                is ClientboundSetEntityDataPacket -> {
                    if (! hide0HealthNames.value || packet.id == mc.player?.id) return@register

                    val hasZeroHealth = packet.packedItems.any { entry ->
                        val value = (entry.value() as? Optional<*>)?.orElse(null)
                        value is Component && healthMatches.any { it.matches(value.formattedText) }
                    }

                    if (hasZeroHealth) {
                        mc.level?.getEntity(packet.id)?.remove(Entity.RemovalReason.DISCARDED)
                        event.isCanceled = true
                    }
                }

                is ClientboundAddEntityPacket -> {
                    val isBlock = packet.type == EntityType.FALLING_BLOCK && hideFallingBlocks.value
                    val isLightning = packet.type == EntityType.LIGHTNING_BOLT && hideLightning.value
                    val isXp = packet.type == EntityType.EXPERIENCE_ORB && hideXpOrbs.value

                    if (isBlock || isLightning || isXp) event.isCanceled = true
                }

                is ClientboundLevelParticlesPacket -> {
                    if (LocationUtils.F7Phase == 5 && hideP5p.value) {
                        if (! packet.particle.type.equalsOneOf(ParticleTypes.ENCHANT, ParticleTypes.FLAME, ParticleTypes.FIREWORK)) {
                            event.isCanceled = true
                        }
                    }
                }

                is ClientboundSetEquipmentPacket -> {
                    if (! LocationUtils.inDungeon) return@register

                    packet.slots.forEach {
                        if (it.first != EquipmentSlot.HEAD) return@forEach
                        val texture = ItemUtils.getSkullTexture(it.second) ?: return@forEach

                        val shouldDiscard = when (texture) {
                            TENTACLE_TEXTURE -> LocationUtils.F7Phase == 5 && removeTentacles.value
                            SOUL_WEAVER_TEXTURE -> hideSoulWeaver.value
                            else -> false
                        }

                        if (shouldDiscard) {
                            mc.level?.getEntity(packet.entity)?.remove(Entity.RemovalReason.DISCARDED)
                        }
                    }
                }
            }
        }

        register<EntityCheckRenderEvent> {
            if (hideDeadMobs.value) {
                if (! event.entity.isAlive || ((event.entity as? LivingEntity)?.health ?: 1f) <= 0) {
                    event.isCanceled = true
                    return@register
                }
            }

            if (! LocationUtils.inDungeon) return@register
            val name = event.entity.displayName?.formattedText ?: return@register
            if (! dungeonMobRegex.matches(name)) return@register

            val isStarred = name.contains("✯")
            if ((isStarred && hideStar.value) || (! isStarred && hideNonStar.value)) {
                event.isCanceled = true
            }
        }
    }
}