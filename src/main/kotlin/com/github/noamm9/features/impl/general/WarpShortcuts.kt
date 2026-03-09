package com.github.noamm9.features.impl.general

import com.github.noamm9.commands.BaseCommand
import com.github.noamm9.commands.CommandManager
import com.github.noamm9.commands.CommandNodeBuilder
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils

object WarpShortcuts: Feature("removes the /warp in warp commands", "Warp Shortcuts") {
    private class WarpCommand(name: String, val loc: String): BaseCommand(name) {
        override fun CommandNodeBuilder.build() {
            requires { enabled }
            runs { ChatUtils.sendCommand("warp $loc") }
        }

        init {
            CommandManager.commands.add(this)
        }
    }

    override fun init() {
        WarpCommand("arachne", "arachne")
        WarpCommand("backwater", "bayou")
        WarpCommand("barn", "barn")
        WarpCommand("base", "camp")
        WarpCommand("bayou", "bayou")
        WarpCommand("blaze", "smold")
        WarpCommand("camp", "camp")
        WarpCommand("carnival", "carnival")
        WarpCommand("castle", "castle")
        WarpCommand("cn", "cn")
        WarpCommand("crypts", "crypts")
        WarpCommand("crystals", "crystals")
        WarpCommand("d", "dhub")
        WarpCommand("da", "da")
        WarpCommand("deep", "deep")
        WarpCommand("desert", "desert")
        WarpCommand("dh", "dhub")
        WarpCommand("dhub", "dhub")
        WarpCommand("dn", "dhub")
        WarpCommand("drag", "drag")
        WarpCommand("dungeon", "dhub")
        WarpCommand("elizabeth", "elizabeth")
        WarpCommand("eman", "void")
        WarpCommand("end", "end")
        WarpCommand("forge", "forge")
        WarpCommand("galatea", "galatea")
        WarpCommand("garden", "garden")
        WarpCommand("gold", "gold")
        WarpCommand("h", "hub")
        WarpCommand("howl", "howl")
        WarpCommand("isle", "isle")
        WarpCommand("jungle", "jungle")
        WarpCommand("kuudra", "kuudra")
        WarpCommand("loch", "murk")
        WarpCommand("mines", "mines")
        WarpCommand("murk", "murk")
        WarpCommand("murkwater", "murk")
        WarpCommand("museum", "museum")
        WarpCommand("nc", "cn")
        WarpCommand("nest", "nest")
        WarpCommand("nether", "isle")
        WarpCommand("park", "park")
        WarpCommand("rift", "rift")
        WarpCommand("skull", "kuudra")
        WarpCommand("smold", "smold")
        WarpCommand("smoldering", "smold")
        WarpCommand("spider", "spider")
        WarpCommand("stonks", "stonks")
        WarpCommand("taylor", "taylor")
        WarpCommand("tomb", "smold")
        WarpCommand("trapper", "trapper")
        WarpCommand("trevor", "trapper")
        WarpCommand("tunnels", "camp")
        WarpCommand("void", "void")
        WarpCommand("wiz", "wizard")
        WarpCommand("wizard", "wizard")
        WarpCommand("wolf", "howl")
        WarpCommand("zombie", "crypts")
    }
}