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
    private val dungeonMobRegex = Regex("^(?:§.)*(.+).+§c❤$")

    private val hideStar by ToggleSetting("Hide Star Mobs's Nametag")
    private val hideNonStar by ToggleSetting("Hide Non Star Mob's Nametag")
    private val hideFallingBlocks by ToggleSetting("Hide Falling Blocks")
    private val hideHealerFairy by ToggleSetting("Hide Healer Fairy")
    private val hideSoulWeaver by ToggleSetting("Hide Soul Weaver")
    private val hide0HealthNames by ToggleSetting("Hide 0 Health")
    private val hideDeadMobs by ToggleSetting("Hide Dead Mobs")
    private val removeTentacles by ToggleSetting("Hide P5 Tentacles")
    private val hideP5p by ToggleSetting("Hide P5 Particles")
    val hideFireOnEntities by ToggleSetting("Hide Fire On Entities")

    private const val TENTACLE_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTcxOTg1NzI3NzI0OSwKICAicHJvZmlsZUlkIiA6ICIxODA1Y2E2MmM0ZDI0M2NiOWQxYmY4YmM5N2E1YjgyNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJSdWxsZWQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzdkODM2NzQ5MjZiODk3MTRlNmI1YTU1NDcwNTAxYzA0YjA2NmRkODdiZjZjMzM1Y2RkYzZlNjBhMWExYTVmNSIKICAgIH0KICB9Cn0="
    private const val HEALER_FAIRY_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTcxOTQ2MzA5MTA0NywKICAicHJvZmlsZUlkIiA6ICIyNjRkYzBlYjVlZGI0ZmI3OTgxNWIyZGY1NGY0OTgyNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJxdWludHVwbGV0IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzJlZWRjZmZjNmExMWEzODM0YTI4ODQ5Y2MzMTZhZjdhMjc1MmEzNzZkNTM2Y2Y4NDAzOWNmNzkxMDhiMTY3YWUiCiAgICB9CiAgfQp9"
    private const val SOUL_WEAVER_TEXTURE = "eyJ0aW1lc3RhbXAiOjE1NTk1ODAzNjI1NTMsInByb2ZpbGVJZCI6ImU3NmYwZDlhZjc4MjQyYzM5NDY2ZDY3MjE3MzBmNDUzIiwicHJvZmlsZU5hbWUiOiJLbGxscmFoIiwic2lnbmF0dXJlUmVxdWlyZWQiOnRydWUsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yZjI0ZWQ2ODc1MzA0ZmE0YTFmMGM3ODViMmNiNmE2YTcyNTYzZTlmM2UyNGVhNTVlMTgxNzg0NTIxMTlhYTY2In19fQ=="

    private val healthMatches = arrayOf(
        Regex("^§.\\[§.Lv\\d+§.] §.+ (?:§.)+0§f/.+§c❤$"),
        Regex("^.+ (?:§.)+0§c❤$")
    )

    override fun init() {
        register<MainThreadPacketReceivedEvent.Pre> {
            if (! LocationUtils.inSkyblock) return@register
            if (event.packet is ClientboundSetEntityDataPacket) {
                if (event.packet.id == mc.player?.id) return@register
                if (! hide0HealthNames.value) return@register
                for (entry in event.packet.packedItems) {
                    val rawValue = entry.value()
                    if (rawValue !is Optional<*>) continue
                    val value = rawValue.orElse(null)
                    if (value !is Component) continue
                    if (healthMatches.none { it.matches(value.formattedText) }) continue

                    mc.level?.getEntity(event.packet.id)?.remove(Entity.RemovalReason.DISCARDED)
                    event.isCanceled = true
                    break
                }
            }
            else if (event.packet is ClientboundAddEntityPacket) {
                if (event.packet.type == EntityType.FALLING_BLOCK && hideFallingBlocks.value) {
                    event.isCanceled = true
                }
            }
            else if (event.packet is ClientboundLevelParticlesPacket) {
                val p = event.packet.particle.type
                if (LocationUtils.F7Phase == 5 && hideP5p.value) {
                    val allowed = p.equalsOneOf(ParticleTypes.ENCHANT, ParticleTypes.FLAME, ParticleTypes.FIREWORK)
                    if (! allowed) event.isCanceled = true
                }
            }
            else if (event.packet is ClientboundSetEquipmentPacket) {
                if (! LocationUtils.inDungeon) return@register
                event.packet.slots.forEach {
                    if (it.first != EquipmentSlot.HEAD) return@forEach
                    val texture = ItemUtils.getSkullTexture(it.second) ?: return@forEach

                    val isTentacle = LocationUtils.F7Phase == 5 && texture == TENTACLE_TEXTURE && removeTentacles.value
                    val isSoulWeaver = texture == SOUL_WEAVER_TEXTURE && hideSoulWeaver.value
                    val isHealerFairy = texture == HEALER_FAIRY_TEXTURE && hideHealerFairy.value

                    if (isTentacle || isSoulWeaver || isHealerFairy) {
                        mc.level?.getEntity(event.packet.entity)?.remove(Entity.RemovalReason.DISCARDED)
                    }
                }
            }
        }

        register<EntityCheckRenderEvent> {
            if (hideDeadMobs.value && ! event.entity.isAlive || ((event.entity as? LivingEntity)?.health ?: 1f) <= 0) {
                event.isCanceled = true
                return@register
            }

            if (! LocationUtils.inDungeon) return@register
            val name = event.entity.displayName?.formattedText ?: return@register
            if (! dungeonMobRegex.matches(name)) return@register

            val isStarred = name.contains("✯")
            if (hideStar.value && isStarred || hideNonStar.value && ! isStarred) {
                event.isCanceled = true
            }
        }
    }
}