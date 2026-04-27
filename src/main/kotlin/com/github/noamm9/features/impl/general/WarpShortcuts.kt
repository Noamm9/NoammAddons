package com.github.noamm9.features.impl.general

import com.github.noamm9.commands.BaseCommand
import com.github.noamm9.commands.CommandManager
import com.github.noamm9.commands.CommandNodeBuilder
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils

object WarpShortcuts: Feature("removes the /warp in warp commands", "Warp Shortcuts") {
    private fun warpCommand(name: String, loc: String) = CommandManager.commands.add(
        object: BaseCommand(name) {
            override fun CommandNodeBuilder.build() {
                requires { enabled }
                runs { ChatUtils.sendCommand("warp $loc") }
            }
        }
    )

    override fun init() {
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
        warpCommand("glowing", "glowing")
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
}