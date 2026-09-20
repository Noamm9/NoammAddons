package com.github.noamm9.features.impl.misc.shit

import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.*
import com.github.noamm9.event.priority.EventPriority
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.RenderHelper.height
import com.github.noamm9.utils.render.RenderHelper.width
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.world.item.ItemStack

@Suppress("unused")
object BlazeDaggerHUD: Feature("Blaze Slayer Dagger Attunement Helper") {
    private val hideWarning by ToggleSetting("Hide Attunement Warning", true).withDescription("Hides the \"Strike using the ... attunement\" chat spam and the Hellion Shield hit-reduction message.")
    private val topDagger by DropdownSetting("Top Line Dagger", 0, Dagger.entries.map { it.displayName }).withDescription("Which dagger family's attunement is shown on the top HUD line.")

    private var clientSideClicked = false
    private var lastDaggerCheckMs = 0L

    private var lastNearest: HellionShield? = null

    private var topText = ""
    private var bottomText = ""

    private val attunementWarningRegex = Regex("§cStrike using the §r.+ §r§cattunement on your dagger!")

    override fun init() {
        hudElement(name = "Dagger Attunement Top", shouldDraw = { topText.isNotEmpty() }, centered = true) { context, isExample ->
            val text = if (isExample) "§7[§eAuric§7] §bCrystal" else topText
            context.drawCenteredString(text, 0, 0)
            return@hudElement text.width().toFloat() to text.height().toFloat()
        }

        hudElement(name = "Dagger Attunement Bottom", shouldDraw = { bottomText.isNotEmpty() }, centered = true) { context, isExample ->
            val text = if (isExample) "§8Ashen §7[§fSPIRIT§7]" else bottomText
            context.drawCenteredString(text, 0, 0)
            return@hudElement text.width().toFloat() to text.height().toFloat()
        }

        register<TickEvent.Start> {
            if (! LocationUtils.inSkyblock) {
                topText = ""
                bottomText = ""
                return@register
            }

            val holding = Dagger.fromStack(mc.player?.mainHandItem)
            if (holding == null) {
                topText = ""
                bottomText = ""
                return@register
            }

            setDaggerText(holding)
        }

        register<PlayerInteractEvent.RIGHT_CLICK.AIR> { onDaggerRightClick(event.item) }
        register<PlayerInteractEvent.RIGHT_CLICK.ENTITY> { onDaggerRightClick(event.item) }
        register<PlayerInteractEvent.RIGHT_CLICK.BLOCK> { onDaggerRightClick(event.item) }

        register<MainThreadPacketReceivedEvent.Pre>(EventPriority.HIGH) {
            val packet = event.packet as? ClientboundSetSubtitleTextPacket ?: return@register
            val titleText = packet.text.unformattedText

            Dagger.updateActiveFromTitle(titleText) ?: return@register

            event.isCanceled = true
            clientSideClicked = false
        }

        register<ChatMessageEvent> {
            if (! hideWarning.value) return@register
            val text = event.formattedText
            if (attunementWarningRegex.matches(text) || text == "§cYour hit was reduced by Hellion Shield!") {
                event.cancel()
            }
        }
    }

    private fun onDaggerRightClick(item: ItemStack?) {
        val dagger = Dagger.fromStack(item) ?: return
        dagger.shields.forEach { it.active = ! it.active }
        clientSideClicked = true
    }

    private fun setDaggerText(holding: Dagger) {
        checkActiveDagger()

        val first = Dagger.entries[topDagger.value]
        val second = first.other()

        topText = format(second, true, holding) + " " + format(first, true, holding)
        bottomText = format(second, false, holding) + " " + format(first, false, holding)
    }

    private fun checkActiveDagger() {
        val now = System.currentTimeMillis()
        if (now - lastDaggerCheckMs < 1000) return
        lastDaggerCheckMs = now

        for (dagger in Dagger.entries) {
            if (dagger.updated) continue
            if (dagger.shields.any { it.active }) continue

            val found = readFromInventory(dagger)
            if (found != null) {
                found.active = true
                dagger.updated = true
            }
            else dagger.shields[0].active = true
        }
    }

    private fun readFromInventory(dagger: Dagger): HellionShield? {
        val items = mc.player?.inventory?.nonEquipmentItems ?: return null

        for (stack in items) {
            if (stack == null || stack.isEmpty) continue
            if (stack.skyblockId !in dagger.skyblockIds) continue

            for (line in stack.lore) {
                if (! line.contains("§7Attuned: ")) continue
                dagger.shields.find { line.contains(it.cleanName) }?.let { return it }
            }
        }

        return null
    }

    private fun format(compareInHand: Dagger, active: Boolean, holding: Dagger): String {
        val inHand = holding == compareInHand

        var shield = compareInHand.activeShield()
        if (! active) shield = shield.other()

        return if (inHand && active) {
            if (lastNearest == null) "§7[${shield.chatColor}${shield.cleanName}§7]"
            else if (shield == lastNearest) "§a[${shield.chatColor}${shield.cleanName.uppercase()}§a]"
            else "§c[§m${shield.chatColor}${shield.cleanName}§c]"
        }
        else if (shield == lastNearest) "§6[${shield.chatColor}${shield.cleanName}§6]"
        else "${shield.chatColor}${shield.cleanName}"
    }
}