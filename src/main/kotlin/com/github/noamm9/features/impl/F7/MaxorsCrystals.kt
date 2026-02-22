package com.github.noamm9.features.impl.F7

import com.github.noamm9.config.PersonalBest
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import com.github.noamm9.ui.clickgui.componnents.withDescription
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.MathUtils
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.PlayerUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.world.entity.EntityType

object MaxorsCrystals: Feature("Utilities for F7 Maxor's Crystals") {
    private val spawnTimer by ToggleSetting("Spawn Timer").withDescription("Shows on screen a Tick Timer on screen for when the crystals with respawn")
    private val placeTimer by ToggleSetting("Place Timer").withDescription("Sends in chat the time took to place the crystal after picking it up")
    private val placeAlert by ToggleSetting("Place Alert").withDescription("Shows on screen when you have a Energy Crystal in your hotbar that u didnt place")

    private val pickupRegex = Regex("^(\\w+) picked up an Energy Crystal!")
    private val spawnRegex = Regex("^\\[BOSS] Maxor: THAT BEAM! IT HURTS! IT HURTS!!$|^\\[BOSS] Maxor: YOU TRICKED ME!$")

    private var pickupTime: Long? = null
    private var tickTimer: Int? = null

    override fun init() {
        register<WorldChangeEvent> {
            pickupTime = null
            tickTimer = null
        }

        register<ChatMessageEvent> {
            val msg = event.unformattedText

            if (placeTimer.value) pickupRegex.find(msg)?.let {
                if (it.destructured.component1() != mc.user.name) return@let
                pickupTime = System.currentTimeMillis()
            }

            if (spawnTimer.value && spawnRegex.matches(msg)) {
                tickTimer = 34
            }
        }

        register<MainThreadPacketReceivedEvent.Post> {
            if (! placeTimer.value) return@register
            if (pickupTime == null) return@register
            val packet = event.packet as? ClientboundAddEntityPacket ?: return@register
            if (packet.type != EntityType.END_CRYSTAL) return@register
            if (packet.y.toInt() != 224) return@register

            val spawnPos = MathUtils.Vec3(packet.x, packet.y, packet.z)
            val distance = MathUtils.distance2D(mc.player !!.position(), spawnPos)
            if (distance >= 5) return@register

            val placeTime = ((System.currentTimeMillis() - pickupTime !!) / 1000.0)
            val oldPB = PersonalBest.getPB("F7_crystal_placement")?.toDouble()
            val isPB = PersonalBest.checkAndSetPB("F7_crystal_placement", placeTime, lowerIsBetter = true)
            var msg = "&aCrystal placed in &e${placeTime.toFixed(3)}s&a."
            if (isPB) msg += " &d&l(PB)"

            ChatUtils.clickableChat(msg, prefix = true, hover = "&7Old Best: &d${oldPB?.toFixed(3) ?: "None"}s")
        }

        register<RenderOverlayEvent> {
            val width = mc.window.guiScaledWidth
            val height = mc.window.guiScaledHeight

            if (spawnTimer.value && tickTimer != null) Render2D.drawCenteredString(
                event.context,
                "&b" + ((tickTimer) !! / 20.0).toFixed(2),
                width / 2,
                height * 0.5 + 10,
                scale = 2.5f,
            )

            if (! placeAlert.value) return@register
            if (LocationUtils.F7Phase != 1) return@register
            if (! PlayerUtils.getHotbarSlot(8)?.hoverName?.unformattedText.equals("energy crystal", true)) return@register
            Render2D.drawCenteredString(event.context, "&e&l⚠ &l&bCrystal &e&l⚠", width / 2, height * 0.2, scale = 3)
        }

        register<TickEvent.Server> {
            if (tickTimer == null) return@register
            tickTimer = when {
                tickTimer !! > 0 -> tickTimer !! - 1
                else -> null
            }
        }
    }
}