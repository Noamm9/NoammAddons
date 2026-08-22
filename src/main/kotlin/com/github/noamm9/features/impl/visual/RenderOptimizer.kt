package com.github.noamm9.features.impl.visual

import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.CheckEntityRenderEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.dungeon.StarMobESP
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.items.ItemUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.startsWithOneOf
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

object RenderOptimizer: Feature("Optimize Rendering by hiding useless stuff.") {
    private val hideStar by ToggleSetting("Hide Star Mobs's Nametag").withDescription("Should probably be used with &b${StarMobESP.name}")
    private val hideNonStar by ToggleSetting("Hide Non Star Mob's Nametag")
    private val hideFallingBlocks by ToggleSetting("Hide Falling Blocks")
    private val hideLightning by ToggleSetting("Hide Lightning Bolts")
    private val hideSoulWeaver by ToggleSetting("Hide Soul Weaver").withDescription("Hides the flying Heads from the Soul Weaver gloves")
    private val hideHealerOrbs by ToggleSetting("Hide Healer Orbs").withDescription("Hides healer support orbs in dungeons. excludes the Damage orb")
    private val hide0HealthNames by ToggleSetting("Hide 0 Health").withDescription("Hide 0 Health nametags")
    private val hideDeadMobs by ToggleSetting("Hide Dead Mobs").withDescription("Hides the mobs death animation.")
    private val hideXpOrbs by ToggleSetting("Hide XP Orbs")
    private val removeTentacles by ToggleSetting("Hide P5 Tentacles").withDescription("Hides the Wither King Tentacles")
    private val hideP5p by ToggleSetting("Hide P5 Particles").withDescription("Hide all Particles in M7 P5 except the relevent ones")
    val hideFireOnEntities by ToggleSetting("Hide Fire On Entities").withDescription("Hides the fire texture on burning mobs")

    private val healthMatches = listOf(Regex("^§.\\[§.Lv\\d+§.] §.+ (?:§.)+0§f/.+§c❤$"), Regex("^.+ (?:§.)+0§c❤$"))
    private val entityNameCache = WeakHashMap<Entity, EntityNameInfo>()

    override fun init() {
        register<MainThreadPacketReceivedEvent.Pre> {
            if (! LocationUtils.inSkyblock) return@register
            when (val packet = event.packet) {
                is ClientboundSetEntityDataPacket -> {
                    if (packet.id == player.id) return@register

                    val name = packet.packedItems.firstNotNullOfOrNull { entry ->
                        (entry.value() as? Optional<*>)?.orElse(null) as? Component
                    }?.formattedText ?: return@register

                    val shouldDiscard = run {
                        val a = hide0HealthNames.value && healthMatches.any { it.matches(name) }
                        val b = hideHealerOrbs.value && name.removeFormatting().startsWithOneOf("DEFENSE", "ABILITY DAMAGE")

                        return@run a || b
                    }

                    if (shouldDiscard) {
                        level.getEntity(packet.id)?.remove(Entity.RemovalReason.DISCARDED)
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
                    if (! hideP5p.value) return@register
                    if (LocationUtils.F7Phase != 5) return@register
                    if (! packet.particle.type.equalsOneOf(ParticleTypes.ENCHANT, ParticleTypes.FLAME, ParticleTypes.FIREWORK))
                        event.isCanceled = true
                }

                is ClientboundSetEquipmentPacket -> {
                    if (! LocationUtils.inDungeon) return@register

                    packet.slots.forEach {
                        if (it.first != EquipmentSlot.HEAD) return@forEach
                        val texture = ItemUtils.getSkullTexture(it.second) ?: return@forEach

                        val shouldDiscard = run {
                            val a = removeTentacles.value && LocationUtils.F7Phase == 5 && texture == TENTACLE_TEXTURE
                            val b = hideSoulWeaver.value && texture == SOUL_WEAVER_TEXTURE
                            val c = hideHealerOrbs.value && texture.equalsOneOf(ABILITY_ORB_TEXTURE, DEFENSE_ORB_TEXTURE)
                            return@run a || b || c
                        }

                        if (shouldDiscard) level.getEntity(packet.entity)?.remove(Entity.RemovalReason.DISCARDED)
                    }
                }
            }
        }

        register<CheckEntityRenderEvent> {
            if (hideDeadMobs.value) {
                if (! event.entity.isAlive || ((event.entity as? LivingEntity)?.health ?: 1f) <= 0) {
                    event.isCanceled = true
                    return@register
                }
            }

            if (! LocationUtils.inDungeon) return@register

            val name = event.entity.customName ?: return@register
            val info = entityNameCache.getOrPut(event.entity) {
                val formatted = name.formattedText
                EntityNameInfo(formatted.contains("✯"), formatted.endsWith("§c❤"))
            }

            if (! info.isHealthTag) return@register

            if ((info.isStarred && hideStar.value) || (! info.isStarred && hideNonStar.value)) {
                event.isCanceled = true
            }
        }
    }

    private data class EntityNameInfo(val isStarred: Boolean, val isHealthTag: Boolean)

    private const val TENTACLE_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTcxOTg1NzI3NzI0OSwKICAicHJvZmlsZUlkIiA6ICIxODA1Y2E2MmM0ZDI0M2NiOWQxYmY4YmM5N2E1YjgyNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJSdWxsZWQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzdkODM2NzQ5MjZiODk3MTRlNmI1YTU1NDcwNTAxYzA0YjA2NmRkODdiZjZjMzM1Y2RkYzZlNjBhMWExYTVmNSIKICAgIH0KICB9Cn0="
    private const val SOUL_WEAVER_TEXTURE = "eyJ0aW1lc3RhbXAiOjE1NTk1ODAzNjI1NTMsInByb2ZpbGVJZCI6ImU3NmYwZDlhZjc4MjQyYzM5NDY2ZDY3MjE3MzBmNDUzIiwicHJvZmlsZU5hbWUiOiJLbGxscmFoIiwic2lnbmF0dXJlUmVxdWlyZWQiOnRydWUsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yZjI0ZWQ2ODc1MzA0ZmE0YTFmMGM3ODViMmNiNmE2YTcyNTYzZTlmM2UyNGVhNTVlMTgxNzg0NTIxMTlhYTY2In19fQ=="
    private const val ABILITY_ORB_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTYzODUyNDAzODE5OCwKICAicHJvZmlsZUlkIiA6ICIzOWEzOTMzZWE4MjU0OGU3ODQwNzQ1YzBjNGY3MjU2ZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJkZW1pbmVjcmFmdGVybG9sIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzVlZTRiYjQ4MjFkMGY1ZWQ4NjVjMjEwOTBhODBiNWVlN2Q1MjI2ODQ3NmVlMjVkMzg5NzEwZjdjYzlmMTEwZDYiCiAgICB9CiAgfQp9"
    private const val DEFENSE_ORB_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTYwNTM1NjUyNzQzOSwKICAicHJvZmlsZUlkIiA6ICJhYTZhNDA5NjU4YTk0MDIwYmU3OGQwN2JkMzVlNTg5MyIsCiAgInByb2ZpbGVOYW1lIiA6ICJiejE0IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzE1NzhiNGFmM2ZkZDkxNTFiODUwYjEzYzY3YzQ1ODAyMjRjN2Y2MDA1MjcxM2YyZDE1MWY3YzE1ZGMwZDdiMzQiCiAgICB9CiAgfQp9"
}
