package noammaddons.features.impl.dungeons

import net.minecraft.network.play.server.S0FPacketSpawnMob
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.noammaddons.Companion.personalBests
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.BlockUtils.toVec
import noammaddons.utils.ChatUtils.clickableChat
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.LocationUtils
import noammaddons.utils.MathUtils
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawCenteredText

object MaxorsCrystals: Feature() {
    private val crystalData get() = personalBests.getData().crystals
    private var tickTimer: Int? = null
    private var pickupTime: Long? = null

    private val spawnTimer by ToggleSetting("Spawn Timer")
    private val placeTimer by ToggleSetting("Place Timer")
    private val placeAlert by ToggleSetting("Place Alert")

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        pickupTime?.let { pickupTime = null }
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        val msg = event.component.noFormatText

        if (placeTimer) Regex("^(\\w+) picked up an Energy Crystal!$").find(msg)?.let {
            if (it.destructured.component1() != mc.session.username) return
            pickupTime = System.currentTimeMillis()
        }

        if (spawnTimer) {
            if (Regex("^\\[BOSS] Maxor: THAT BEAM! IT HURTS! IT HURTS!!$|^\\[BOSS] Maxor: YOU TRICKED ME!$").matches(msg)) {
                tickTimer = 34
            }
        }
    }

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Received) {
        if (! placeTimer) return
        if (pickupTime == null) return
        val packet = event.packet as? S0FPacketSpawnMob ?: return
        if (packet.entityType != 200) return // Ender Crystal
        if (packet.y != 224) return
        val spawnPos = BlockPos(packet.x, packet.y, packet.z).toVec()
        val distance = MathUtils.distance2D(mc.thePlayer.positionVector, spawnPos)
        if (distance >= 5) return

        val placeTime = ((System.currentTimeMillis() - pickupTime !!) / 1000.0)
        var msg = "&aCrystal placed in &e${placeTime.toFixed(3)}s&a."
        val crystalTime = crystalData[0]
        if (placeTime < (crystalTime ?: Double.MAX_VALUE) || crystalTime == null) {
            crystalData[0] = placeTime
            personalBests.save()
            msg += " &d&l(PB)"
        }

        clickableChat(
            msg,
            prefix = true,
            hover = "&dPersonal Best: &a${crystalTime?.toFixed(3)}s",
        )
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        if (tickTimer == null) return
        tickTimer = when {
            tickTimer !! > 0 -> tickTimer !! - 1
            else -> null
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (spawnTimer && tickTimer != null) drawCenteredText(
            "&b" + (tickTimer !! / 20.0).toFixed(2),
            mc.getWidth() / 2,
            mc.getHeight() * 0.5 + 10,
            2.5f,
        )

        if (! placeAlert) return
        if (LocationUtils.F7Phase != 1) return
        if (mc.thePlayer.inventory.getStackInSlot(8)
                ?.displayName?.removeFormatting()
                ?.lowercase() != "energy crystal"
        ) return

        drawCenteredText("&e&l⚠ &l&bCrystal &e&l⚠", mc.getWidth() / 2, mc.getHeight() * 0.2, 3)
    }
}
