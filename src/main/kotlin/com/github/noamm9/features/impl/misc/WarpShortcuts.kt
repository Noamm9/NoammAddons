package com.github.noamm9.features.impl.misc

import com.github.noamm9.commands.BaseCommand
import com.github.noamm9.commands.CommandManager
import com.github.noamm9.commands.CommandNodeBuilder
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils

object WarpShortcuts : Feature("removes the /warp in warp commands", "Warp Shortcuts") {
    val commands = mutableListOf<WarpCommand>()

    class WarpCommand(var input: String, var output: String) : BaseCommand(input) {
        override fun CommandNodeBuilder.build() {
            requires { enabled }
            runs { warp(output) }
        }

        private fun warp(destination: String) {
            if (enabled) ChatUtils.sendCommand("warp $destination")
            else return
        }
    }

    override fun init() {
        CommandManager.commands.removeIf { it is WarpCommand }
        commands.clear()
        commands.addAll(
            listOf(
                WarpCommand("arachne", "arachne"),
                WarpCommand("barn", "barn"),
                WarpCommand("bayou", "bayou"),
                WarpCommand("back", "bayou"),
                WarpCommand("backwater", "bayou"),
                WarpCommand("camp", "camp"),
                WarpCommand("base", "camp"),
                WarpCommand("tunnels", "camp"),
                WarpCommand("castle", "castle"),
                WarpCommand("cn", "cn"),
                WarpCommand("nc", "cn"),
                WarpCommand("carnival", "carnival"),
                WarpCommand("crypts", "crypts"),
                WarpCommand("zombie", "crypts"),
                WarpCommand("da", "da"),
                WarpCommand("deep", "deep"),
                WarpCommand("desert", "desert"),
                WarpCommand("crystals", "crystals"),
                WarpCommand("dhub", "dhub"),
                WarpCommand("dn", "dhub"),
                WarpCommand("dh", "dhub"),
                WarpCommand("d", "dhub"),
                WarpCommand("dungeon", "dhub"),
                WarpCommand("drag", "drag"),
                WarpCommand("elizabeth", "elizabeth"),
                WarpCommand("end", "end"),
                WarpCommand("forge", "forge"),
                WarpCommand("galatea", "galatea"),
                WarpCommand("garden", "garden"),
                WarpCommand("gold", "gold"),
                WarpCommand("howl", "howl"),
                WarpCommand("wolf", "howl"),
                WarpCommand("h", "hub"),
                WarpCommand("isle", "isle"),
                WarpCommand("nether", "isle"),
                WarpCommand("jerry", "jerry"),
                WarpCommand("jungle", "jungle"),
                WarpCommand("kuudra", "kuudra"),
                WarpCommand("skull", "kuudra"),
                WarpCommand("mines", "mines"),
                WarpCommand("murk", "murk"),
                WarpCommand("murkwater", "murk"),
                WarpCommand("loch", "murk"),
                WarpCommand("museum", "museum"),
                WarpCommand("nest", "nest"),
                WarpCommand("park", "park"),
                WarpCommand("rift", "rift"),
                WarpCommand("spider", "spider"),
                WarpCommand("stonks", "stonks"),
                WarpCommand("tomb", "tomb"),
                WarpCommand("smold", "tomb"),
                WarpCommand("smoldering", "tomb"),
                WarpCommand("blaze", "tomb"),
                WarpCommand("trapper", "trapper"),
                WarpCommand("trevor", "trapper"),
                WarpCommand("taylor", "taylor"),
                WarpCommand("void", "void"),
                WarpCommand("eman", "void"),
                WarpCommand("wiz", "wizard"),
                WarpCommand("wizard", "wizard"),
            )
        )
        CommandManager.commands.addAll(commands)
    }
}