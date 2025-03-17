package noammaddons.features.dungeons

import net.minecraft.network.play.server.S0FPacketSpawnMob
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.DungeonUtils.Classes.*
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.MathUtils.distance2D
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawCenteredText
import kotlin.math.ceil

object DungeonAbilityCooldowns: Feature() {
    private val cooldownReductionRegex = Regex("^\\[Mage] Cooldown Reduction \\d+% -> (\\d+)%\$")
    private var currentCooldown: Double? = null
    private var mageCooldownReduction: Int? = null

    private fun displayCooldown(baseDuration: Int) {
        val player = thePlayer ?: return
        val baseCooldown = (baseDuration + 1) * 20.0

        currentCooldown = when (player.clazz) {
            Archer -> baseCooldown - (player.clazzLvl / 5) * 2

            Tank -> {
                if (mc.thePlayer.heldItem?.SkyblockID == "EARTH_SHARD") baseCooldown - 40
                else baseCooldown
            }

            Mage -> ceil(
                if (mageCooldownReduction == null) (25 + (player.clazzLvl / 2)) / 100.0
                else mageCooldownReduction !! / 100.0 * baseCooldown
            )

            else -> baseCooldown
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! config.dungeonAbilityCooldowns) return
        currentCooldown?.let { cooldown ->
            drawCenteredText(
                "${cooldown.toInt() / 20}",
                mc.getWidth() / 2,
                mc.getHeight() / 2 + 10
            )
        }
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        currentCooldown = currentCooldown?.let {
            if (it > 0) it - 1
            else null
        }
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! inDungeon) return
        val message = event.component.noFormatText

        cooldownReductionRegex.find(message)?.run {
            mageCooldownReduction = groupValues[1].toInt() - 18
        }

        when (message) {
            "Used Explosive Shot!" -> displayCooldown(40)
            "Used Seismic Wave!" -> displayCooldown(15)
        }
    }

    // Mage's sheep detection, I wanna KMS
    @SubscribeEvent
    fun onPacket(event: PacketEvent.Received) {
        if (event.packet !is S0FPacketSpawnMob) return
        if (event.packet.entityType != 91) return
        val player = thePlayer ?: return
        if (player.clazz != Mage) return
        val sheepPosition = Vec3(event.packet.x.toDouble(), event.packet.y.toDouble(), event.packet.z.toDouble())
        player.entity?.run {
            val distance = distance2D(positionVector, sheepPosition)
            if (distance > 7000) return // Hypixel wtf?
            displayCooldown(30)
        }
    }
}

