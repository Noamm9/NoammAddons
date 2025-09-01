package noammaddons.features.impl.dungeons

import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.server.S0EPacketSpawnObject
import net.minecraft.network.play.server.S0FPacketSpawnMob
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.features.Feature
import noammaddons.features.impl.misc.DamageSplash.damageRegex
import noammaddons.ui.config.core.annotations.Dev
import noammaddons.ui.config.core.impl.SeperatorSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.LocationUtils.inDungeon

@Dev
object FpsBoost: Feature() {
    val dungeonMobRegex = Regex("^(?:§.)*(.+)§r.+§c❤\$")

    private val hideStar = ToggleSetting("Hide Star Mobs's Nametag")
    private val hideNonStar = ToggleSetting("Hide Non Star Mob's Nametag")
    private val hideDamageNumbers = ToggleSetting("Hide Damage Numbers")
    private val hideFallingBlocks = ToggleSetting("Hide Falling Blocks")

    @JvmField
    val hideFireOnEntities = ToggleSetting("Hide Fire On Entities")

    override fun init() = addSettings(
        SeperatorSetting("Dungeons"),
        hideNonStar, hideStar,
        SeperatorSetting("Other"),
        hideDamageNumbers, hideFallingBlocks,
        hideFireOnEntities
    )

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Received) {
        if (event.packet is S0EPacketSpawnObject) {
            if (! hideFallingBlocks.value) return
            if (event.packet.type != 70) return
            event.isCanceled = true
        }
        else if (event.packet is S0FPacketSpawnMob) {
            if (! hideDamageNumbers.value) return
            if (event.packet.entityType != 30) return
            val nameData = event.packet.func_149027_c()?.find { it.getObject().toString().contains("§") } ?: return
            val name = "${nameData.getObject()}".removeFormatting()
            if (! name.matches(damageRegex)) return
            event.isCanceled = true
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onRenderNameTag(event: RenderLivingEvent.Specials.Pre<EntityArmorStand>) {
        if (! inDungeon) return
        if (! event.entity.hasCustomName()) return
        val name = event.entity.customNameTag
        if (! name.matches(dungeonMobRegex)) return
        val isStarred = name.contains("§6✯")
        if (isStarred && hideStar.value) return cancel(event)
        else if (! isStarred && hideNonStar.value) return cancel(event)
    }

    private fun cancel(event: RenderLivingEvent.Specials.Pre<*>) {
        event.entity.alwaysRenderNameTag = false
        event.isCanceled = true
    }
}
