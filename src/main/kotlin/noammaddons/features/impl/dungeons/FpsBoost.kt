package noammaddons.features.impl.dungeons

import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.item.EntityItem
import net.minecraft.init.Items
import net.minecraft.network.play.server.*
import net.minecraft.util.EnumParticleTypes.*
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.events.RenderEntityEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.annotations.Dev
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.ItemUtils.getSkullTexture
import noammaddons.utils.LocationUtils
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.ScanUtils
import noammaddons.utils.Utils.equalsOneOf

@Dev
object FpsBoost: Feature() {
    val dungeonMobRegex = Regex("^(?:§.)*(.+)§r.+§c❤\$")

    private val hideStar = ToggleSetting("Hide Star Mobs's Nametag")
    private val hideNonStar = ToggleSetting("Hide Non Star Mob's Nametag")
    private val hideDamageNumbers = ToggleSetting("Hide Damage Numbers")
    private val hideFallingBlocks = ToggleSetting("Hide Falling Blocks")

    @JvmField
    val hideFireOnEntities = ToggleSetting("Hide Fire On Entities")

    private val removeTentacles = ToggleSetting("Remove P5 Tentacles")
    private val hideHealerFairy = ToggleSetting("Hide Healer Fairy")
    private val hideSoulWeaver = ToggleSetting("Hide Soul Weaver")
    private val hideArcherBones = ToggleSetting("Hide Archer Bones")
    private val hide0HealthNames = ToggleSetting("Hide 0 Health")
    private val hideDeadMobs = ToggleSetting("Hide Dead Mobs")

    private val showParticleOptions = MultiCheckboxSetting("Particles Options", mapOf("Hide P5 Particles" to false, "Remove Explosion" to false, "Hide Heart Particles" to false))

    private const val TENTACLE_TEXTURE =
        "ewogICJ0aW1lc3RhbXAiIDogMTcxOTg1NzI3NzI0OSwKICAicHJvZmlsZUlkIiA6ICIxODA1Y2E2MmM0ZDI0M2NiOWQxYmY4YmM5N2E1YjgyNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJSdWxsZWQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzdkODM2NzQ5MjZiODk3MTRlNmI1YTU1NDcwNTAxYzA0YjA2NmRkODdiZjZjMzM1Y2RkYzZlNjBhMWExYTVmNSIKICAgIH0KICB9Cn0="
    private const val HEALER_FAIRY_TEXTURE =
        "ewogICJ0aW1lc3RhbXAiIDogMTcxOTQ2MzA5MTA0NywKICAicHJvZmlsZUlkIiA6ICIyNjRkYzBlYjVlZGI0ZmI3OTgxNWIyZGY1NGY0OTgyNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJxdWludHVwbGV0IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzJlZWRjZmZjNmExMWEzODM0YTI4ODQ5Y2MzMTZhZjdhMjc1MmEzNzZkNTM2Y2Y4NDAzOWNmNzkxMDhiMTY3YWUiCiAgICB9CiAgfQp9"
    private const val SOUL_WEAVER_TEXTURE =
        "eyJ0aW1lc3RhbXAiOjE1NTk1ODAzNjI1NTMsInByb2ZpbGVJZCI6ImU3NmYwZDlhZjc4MjQyYzM5NDY2ZDY3MjE3MzBmNDUzIiwicHJvZmlsZU5hbWUiOiJLbGxscmFoIiwic2lnbmF0dXJlUmVxdWlyZWQiOnRydWUsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yZjI0ZWQ2ODc1MzA0ZmE0YTFmMGM3ODViMmNiNmE2YTcyNTYzZTlmM2UyNGVhNTVlMTgxNzg0NTIxMTlhYTY2In19fQ=="

    private val healthMatches = arrayOf(
        Regex("^§.\\[§.Lv\\d+§.] §.+ (?:§.)+0§f/.+§c❤$"),
        Regex("^.+ (?:§.)+0§c❤$")
    )

    override fun init() = addSettings(
        SeperatorSetting("Dungeons"),
        hideNonStar, hideStar, removeTentacles,
        hideHealerFairy, hideSoulWeaver,
        hideArcherBones, hide0HealthNames,
        hideDeadMobs,
        SeperatorSetting("Other"),
        showParticleOptions, hideDamageNumbers,
        hideFallingBlocks, hideFireOnEntities
    )


    @SubscribeEvent
    fun onPacket(event: PacketEvent.Received) = with(event.packet) {
        if (! LocationUtils.inSkyblock) return@with
        if (this is S1CPacketEntityMetadata && hide0HealthNames.value) {
            mc.theWorld?.getEntityByID(entityId)?.let { entity ->
                func_149376_c()?.find { it.objectType == 4 }?.getObject()?.toString()?.let { name ->
                    if (healthMatches.any { regex -> regex.matches(name) }) entity.setDead()
                }

                (func_149376_c()?.find { it.dataValueId == 6 }?.getObject() as? Float)?.let { health ->
                    if (hideDeadMobs.value && health <= 0) entity.setDead()
                }
            }
        }

        if (this is S0EPacketSpawnObject && type == 70 && hideFallingBlocks.value) event.isCanceled = true

        if (this is S2APacketParticles) {
            if (particleType.equalsOneOf(EXPLOSION_NORMAL, EXPLOSION_LARGE, EXPLOSION_HUGE) && showParticleOptions.get("Remove Explosion"))
                event.isCanceled = true

            if (F7Phase == 5 && showParticleOptions.get("Hide P5 Particles") && ! particleType.equalsOneOf(ENCHANTMENT_TABLE, FLAME, FIREWORKS_SPARK))
                event.isCanceled = true

            if (showParticleOptions.get("Hide Heart Particles") && particleType == HEART)
                event.isCanceled = true
        }

        if (this is S04PacketEntityEquipment && inDungeon) {
            val entity = mc.theWorld.getEntityByID(entityID) ?: return
            val isTentacle = F7Phase == 5 && equipmentSlot == 4 && getSkullTexture(itemStack) == TENTACLE_TEXTURE
            val isSoulWeaver = equipmentSlot == 4 && getSkullTexture(itemStack) == SOUL_WEAVER_TEXTURE
            val isHealerFairy = equipmentSlot == 0 && getSkullTexture(itemStack) == HEALER_FAIRY_TEXTURE
            if (! isTentacle && ! isSoulWeaver && ! isHealerFairy) return
            event.isCanceled = true
            entity.setDead()
        }
    }

    @SubscribeEvent
    fun onPacketPost(event: RenderEntityEvent) {
        if (! inDungeon || inBoss) return
        event.entity.takeIf { it is EntityArmorStand }?.customNameTag?.takeIf { it.matches(dungeonMobRegex) }?.let { name ->
            if (! name.matches(dungeonMobRegex)) return@let
            if (ScanUtils.getEntityRoom(event.entity)?.data?.name?.contains("Blaze") == true) return@let
            val isStarred = name.contains("§6✯")
            if (hideStar.value && isStarred || hideNonStar.value && ! isStarred) {
                event.entity.setDead()
                event.isCanceled = true
            }
        }

        val entityItem = (event.entity as? EntityItem)?.entityItem ?: return
        if (entityItem.itemDamage == 15 && entityItem.item === Items.dye) {
            event.entity.setDead()
            event.isCanceled = true
        }
    }
}