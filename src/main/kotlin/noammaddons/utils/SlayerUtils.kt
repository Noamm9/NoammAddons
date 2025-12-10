package noammaddons.utils

import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.*
import net.minecraft.entity.passive.EntityWolf
import net.minecraft.network.play.server.S1CPacketEntityMetadata
import net.minecraft.network.play.server.S3EPacketTeams
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.NoammAddons.Companion.mc
import noammaddons.events.*
import noammaddons.features.impl.slayers.SlayerFeatures.slayerBossSpawnTime
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import java.util.concurrent.CopyOnWriteArraySet

object SlayerUtils {
    private val killRegex = Regex(" (\\d+)\\/(\\d+) Kills")

    var isQuestActive = false
    var currentMobKills = 0
    var maxMobKills = 0
    var isFightingBoss = false

    val slayerBossEntity: SlayerEntity = SlayerEntity(BossType.NONE, - 69696)
    val slayerMinibosses = BossType.entries.associateWith { CopyOnWriteArraySet<Int>() }

    @SubscribeEvent
    fun onPacket(event: MainThreadPacketRecivedEvent.Post) {
        if (! isQuestActive || ! LocationUtils.inSkyblock || LocationUtils.inDungeon) return
        val packet = event.packet as? S1CPacketEntityMetadata ?: return
        val entity = mc.theWorld.getEntityByID(packet.entityId) as? EntityArmorStand ?: return
        val name = entity.customNameTag?.removeFormatting() ?: return

        if (! isFightingBoss) {
            if (name.contains("Spawned by") && name.endsWith("by: ${mc.session.username}")) {
                isFightingBoss = true
                // The Spawned By armorstand is always ID+3 from the actual boss entity
                slayerBossEntity.entityId = packet.entityId - 3
                slayerBossEntity.type = BossType.getTypeByEntity(slayerBossEntity.entity)
                slayerBossSpawnTime = System.currentTimeMillis()

                EventDispatcher.postAndCatch(SlayerEvent.BossSpawnEvent())
            }
        }

        BossType.entries.find { it.minibosses.any { mbName -> name.contains(mbName, true) } }?.let { bossType ->
            // The nametag armor stand is usually ID+1 from the mob
            val mobId = packet.entityId - 1

            if (slayerMinibosses[bossType]?.add(mobId) == true) {
                EventDispatcher.postAndCatch(SlayerEvent.MiniBossSpawnEvent(mobId, bossType))
            }
        }
    }

    @SubscribeEvent
    fun onScoreBoardUpdate(event: MainThreadPacketRecivedEvent.Pre) {
        if (! LocationUtils.inSkyblock) return
        if (LocationUtils.inDungeon) return
        val packet = event.packet as? S3EPacketTeams ?: return
        if (packet.action != 2) return
        val text = ScoreboardUtils.cleanSB(packet.prefix?.plus(packet.suffix) ?: return)
        if (text.contains("Slayer Quest") && ! isQuestActive) isQuestActive = true

        killRegex.find(text)?.destructured?.let {
            val killsInt = it.component1().toIntOrNull() ?: return
            val maxKillsInt = it.component2().toIntOrNull() ?: return
            if (killsInt != currentMobKills) currentMobKills = killsInt
            if (maxKillsInt != maxMobKills) maxMobKills = maxKillsInt
        }
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! LocationUtils.inSkyblock) return
        if (LocationUtils.inDungeon) return
        when (event.component.noFormatText.trim()) {
            "SLAYER QUEST FAILED!", "SLAYER QUEST COMPLETE!", "Your Slayer Quest has been cancelled!" -> {
                isQuestActive = false
                slayerBossEntity.reset()
                isFightingBoss = false
                EventDispatcher.postAndCatch(SlayerEvent.QuestEndEvent())
            }

            "SLAYER QUEST STARTED!" -> {
                isQuestActive = true
                slayerBossEntity.reset()
                isFightingBoss = false
                EventDispatcher.postAndCatch(SlayerEvent.QuestStartEvent())
            }
        }
    }

    @SubscribeEvent
    fun onLivingDeath(event: LivingDeathEvent) {
        if (! LocationUtils.inSkyblock) return
        if (LocationUtils.inDungeon) return

        if (event.entity.entityId == slayerBossEntity.entityId) {
            EventDispatcher.postAndCatch(SlayerEvent.BossDeathEvent())
            isFightingBoss = false
            slayerBossEntity.reset()
        }

        slayerMinibosses.values.find { event.entity.entityId in it }?.remove(event.entity.entityId)
    }


    enum class BossType(val displayName: String, val minibosses: Array<String>) {
        REV("§2Revenant§r", arrayOf("Revenant Sycophant", "Revenant Champion", "Deformed Revenant", "Atoned Champion", "Atoned Revenant")),
        SPIDER("§cTarantula§r", arrayOf("Tarantula Vermin", "Tarantula Beast", "Mutant Tarantula")),
        SVEN("§cSven§r", arrayOf("Pack Enforcer", "Sven Follower", "Sven Alpha")),
        EMAN("§5Voidgloom§r", arrayOf("Voidling Devotee", "Voidling Radical", "Voidcrazed Maniac")),
        BLAZE("§6Inferno Demonlord§r", arrayOf("Flare Demon", "Kindleheart Demon", "Burningsoul Demon")),
        VAMP("§4Riftstalker Bloodfiend§r", arrayOf()),
        NONE("", arrayOf());

        companion object {
            fun getTypeByEntity(entity: Entity) = when (entity) {
                is EntityZombie -> REV
                is EntitySpider -> SPIDER
                is EntityWolf -> SVEN
                is EntityEnderman -> EMAN
                is EntityBlaze -> BLAZE
                is EntityOtherPlayerMP -> {
                    if (entity.name == "Bloodfiend ") VAMP
                    else NONE
                }

                else -> NONE
            }
        }
    }

    data class SlayerEntity(var type: BossType, var entityId: Int) {
        val entity get() = mc.theWorld.getEntityByID(entityId)

        fun reset() {
            type = BossType.NONE
            entityId = - 69696
        }
    }
}