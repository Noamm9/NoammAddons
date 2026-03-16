package com.github.noamm9.features.impl.general

import com.github.noamm9.commands.BaseCommand
import com.github.noamm9.commands.CommandManager
import com.github.noamm9.commands.CommandNodeBuilder
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.section
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import org.apache.commons.lang3.StringUtils.center
import javax.xml.stream.Location

object WarpShortcuts: Feature("removes the /warp in warp commands", "Warp Shortcuts") {
    private val warpCooldown by ToggleSetting("Warp Cooldown HUD", false).section("Draw")
    private var tickCounter = 0
    private var worldChange = false

    private fun warpCommand(name: String, loc: String) = CommandManager.commands.add(
        object: BaseCommand(name) {
            override fun CommandNodeBuilder.build() {
                requires { enabled }
                runs { ChatUtils.sendCommand("warp $loc") }
            }
        }
    )

    override fun init() {

        hudElement("Warp Cooldown", enabled = { warpCooldown.value }, shouldDraw = { LocationUtils.inSkyblock }, centered = true) { ctx, example ->
            val textToRender = if (example) "&bWarp Coolodown&f: &a4.67"
            else {
                if (! worldChange) return@hudElement 0f to 0f
                val secondsLeft = (100 - tickCounter) / 20.0
                val color = when {
                    secondsLeft > 3 -> "&a"
                    secondsLeft >= 1.5 -> "&6"
                    else -> "&c"
                }
                "&bWarp Cooldown&f: ${color + secondsLeft.toFixed(2)}"
            }
            Render2D.drawCenteredString(ctx, textToRender, 0f, 0f)
            return@hudElement textToRender.width().toFloat() to 9F
        }

        register<WorldChangeEvent> {
            worldChange = true
            tickCounter = 0
        }

        register<TickEvent.Server> {
            if (worldChange)
                tickCounter ++
            if (tickCounter == 100) {
                worldChange = false
                tickCounter = 0
            }
        }

        warpCommand("arachne", "arachne")
        warpCommand("backwater", "bayou")
        warpCommand("barn", "barn")
        warpCommand("base", "camp")
        warpCommand("bayou", "bayou")
        warpCommand("blaze", "smold")
        warpCommand("camp", "camp")
        warpCommand("carnival", "carnival")
        warpCommand("castle", "castle")
        warpCommand("cn", "cn")
        warpCommand("crypts", "crypts")
        warpCommand("crystals", "crystals")
        warpCommand("d", "dhub")
        warpCommand("da", "da")
        warpCommand("deep", "deep")
        warpCommand("desert", "desert")
        warpCommand("dh", "dhub")
        warpCommand("dhub", "dhub")
        warpCommand("dn", "dhub")
        warpCommand("drag", "drag")
        warpCommand("dungeon", "dhub")
        warpCommand("elizabeth", "elizabeth")
        warpCommand("eman", "void")
        warpCommand("end", "end")
        warpCommand("forge", "forge")
        warpCommand("galatea", "galatea")
        warpCommand("garden", "garden")
        warpCommand("gold", "gold")
        warpCommand("h", "hub")
        warpCommand("howl", "howl")
        warpCommand("isle", "isle")
        warpCommand("jungle", "jungle")
        warpCommand("kuudra", "kuudra")
        warpCommand("loch", "murk")
        warpCommand("mines", "mines")
        warpCommand("murk", "murk")
        warpCommand("murkwater", "murk")
        warpCommand("museum", "museum")
        warpCommand("nc", "cn")
        warpCommand("nest", "nest")
        warpCommand("nether", "isle")
        warpCommand("park", "park")
        warpCommand("rift", "rift")
        warpCommand("skull", "kuudra")
        warpCommand("smold", "smold")
        warpCommand("smoldering", "smold")
        warpCommand("spider", "spider")
        warpCommand("stonks", "stonks")
        warpCommand("taylor", "taylor")
        warpCommand("tomb", "smold")
        warpCommand("trapper", "trapper")
        warpCommand("trevor", "trapper")
        warpCommand("tunnels", "camp")
        warpCommand("void", "void")
        warpCommand("wiz", "wizard")
        warpCommand("wizard", "wizard")
        warpCommand("wolf", "howl")
        warpCommand("zombie", "crypts")
    }

    val warpList = listOf(
        "arachne" to "arachne",
        "backwater" to "backwater bayou",
        "bayou" to "backwater bayou",
        "barn" to "barn",
        "base" to "camp",
        "camp" to "camp",
        "tunnels" to "camp",
        "carnival" to "carnival",
        "castle" to "castle",
        "isle" to "crimson isles",
        "nether" to "crimson isles",
        "crystals" to "crystal hollows",
        "cn" to "crystal nucleus",
        "nc" to "crystal nucleus",
        "crypts" to "crypts",
        "zombie" to "crypts",
        "da" to "dark auction",
        "deep" to "deep caverns",
        "desert" to "desert",
        "drag" to "dragons gate",
        "d" to "dungeon hub",
        "dh" to "dungeon hub",
        "dhub" to "dungeon hub",
        "dn" to "dungeon hub",
        "dungeon" to "dungeon hub",
        "mines" to "dwarven mines",
        "elizabeth" to "elizabeth",
        "end" to "end",
        "forge" to "forge",
        "galatea" to "galatea",
        "garden" to "garden",
        "gold" to "gold mines",
        "howl" to "howling cave",
        "wolf" to "howling cave",
        "h" to "hub",
        "jungle" to "jungle island",
        "kuudra" to "kuudra's skull",
        "skull" to "kuudra's skull",
        "loch" to "murkwater",
        "murk" to "murkwater",
        "murkwater" to "murkwater",
        "museum" to "museum",
        "park" to "park",
        "rift" to "rift",
        "blaze" to "smoldering cave",
        "smold" to "smoldering cave",
        "smoldering" to "smoldering cave",
        "tomb" to "smoldering cave",
        "nest" to "spider's nest",
        "spider" to "spider's nest",
        "stonks" to "stonks",
        "taylor" to "taylor",
        "trapper" to "trapper",
        "trevor" to "trapper",
        "eman" to "void sepulture",
        "void" to "void sepulture",
        "wiz" to "wizard tower",
        "wizard" to "wizard tower"
    )
}