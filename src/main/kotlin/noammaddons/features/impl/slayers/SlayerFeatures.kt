package noammaddons.features.impl.slayers

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderEntityEvent
import noammaddons.events.SlayerEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ColorSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.modMessage
import java.awt.Color

object SlayerFeatures: Feature() {
    val minibossAlert by ToggleSetting("Miniboss Alert")
    val highlightSlayerBoss by ToggleSetting("Hightlight Boss")
    val highlightSlayerBossColor by ColorSetting("Boss Hightlight Color", Color.WHITE, false).hideIf { ! highlightSlayerBoss }
    val highlightMinibosses by ToggleSetting("Hightlight Minibosses")
    val highlightMinibossesColor by ColorSetting("Minibosses Hightlight Color", Color.RED, false).hideIf { ! highlightMinibosses }

    var slayerBossSpawnTime = System.currentTimeMillis()

    @SubscribeEvent
    fun onSlayerMinibossSpawn(event: SlayerEvent.MiniBossSpawnEvent) {
        if (! minibossAlert) return
        if (! SlayerUtils.isQuestActive) return
        ChatUtils.showTitle("&c&lMiniBoss!")
        repeat(2) { mc.thePlayer.playSound("random.orb", 1f, 0f) }
    }

    @SubscribeEvent
    fun onRenderEntity(event: RenderEntityEvent) {
        if (! SlayerUtils.isQuestActive) return

        if (highlightSlayerBoss && SlayerUtils.slayerBossEntity.entityId == event.entity.entityId) {
            EspUtils.espMob(event.entity, highlightSlayerBossColor)
        }
        else if (highlightMinibosses && event.entity.entityId in SlayerUtils.slayerMinibosses.values.flatten()) {
            EspUtils.espMob(event.entity, highlightMinibossesColor)
        }
    }

    @SubscribeEvent
    fun onSlayerBossSpawn(event: SlayerEvent.BossSpawnEvent) {
        if (SlayerUtils.slayerBossEntity.type == SlayerUtils.BossType.SPIDER) {
            if (SlayerUtils.slayerBossEntity.entity.name == "Dinnerbone") return
        }
        slayerBossSpawnTime = System.currentTimeMillis()
    }

    @SubscribeEvent
    fun onSlayerBossDeath(event: SlayerEvent.BossDeathEvent) {
        if (SlayerUtils.slayerBossEntity.type == SlayerUtils.BossType.SPIDER) {
            if (SlayerUtils.slayerBossEntity.entity.name != "Dinnerbone") return
        }

        modMessage(
            "${SlayerUtils.slayerBossEntity.type.displayName} &bBoss Took:&f ${
                NumbersUtils.formatMilis(System.currentTimeMillis() - slayerBossSpawnTime)
            } &bto kill"
        )
    }
}