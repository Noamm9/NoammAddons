package noammaddons.features.impl.dungeons

import net.minecraft.network.play.server.S0FPacketSpawnMob
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.noammaddons.Companion.personalBests
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.BlockUtils.toVec
import noammaddons.utils.ChatUtils.clickableChat
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

    init {
        onWorldLoad { pickupTime?.let { pickupTime = null } }
        onChat(Regex("^(\\w+) picked up an Energy Crystal!$"), { placeTimer }) {
            if (it.destructured.component1() != mc.session.username) return@onChat
            pickupTime = System.currentTimeMillis()
        }
        onPacket<S0FPacketSpawnMob>({ placeTimer }) { packet ->
            if (pickupTime == null) return@onPacket
            if (packet.entityType != 200) return@onPacket // Ender Crystal
            if (packet.y != 224) return@onPacket
            val spawnPos = BlockPos(packet.x, packet.y, packet.z).toVec()
            val distance = MathUtils.distance2D(mc.thePlayer.positionVector, spawnPos)
            if (distance >= 5) return@onPacket

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
        onServerTick({ tickTimer != null }) {
            tickTimer = when {
                tickTimer !! > 0 -> tickTimer !! - 1
                else -> null
            }
        }
        onChat(Regex("^\\[BOSS] Maxor: THAT BEAM! IT HURTS! IT HURTS!!$|^\\[BOSS] Maxor: YOU TRICKED ME!$"), { spawnTimer }) {
            tickTimer = 34
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! spawnTimer) return
        if (tickTimer == null) return
        drawCenteredText(
            "&b" + (tickTimer !! / 20.0).toFixed(2),
            mc.getWidth() / 2,
            mc.getHeight() * 0.5 + 10,
            2.5f,
        )
    }

    @SubscribeEvent
    fun title(event: RenderOverlay) {
        if (! placeAlert) return
        if (LocationUtils.F7Phase != 1) return
        if (mc.thePlayer.inventory.getStackInSlot(8)
                ?.displayName?.removeFormatting()
                ?.lowercase() != "energy crystal"
        ) return

        drawCenteredText("&e&l⚠ &l&bCrystal &e&l⚠", mc.getWidth() / 2, mc.getHeight() * 0.2, 3)
    }
}
