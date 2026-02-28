package com.github.noamm9.features.impl.misc

import com.github.noamm9.commands.BaseCommand
import com.github.noamm9.commands.CommandManager
import com.github.noamm9.commands.CommandNodeBuilder
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils

object WarpShortcuts : Feature("removes the /warp in warp commands", "Warp Shortcuts") {
    val commands = mutableListOf<WarpUtils>()

    class WarpUtils(var input: String, var output: String) : BaseCommand(input) {
        override fun CommandNodeBuilder.build() {
            requires { WarpShortcuts.enabled }
            runs { warp(output) }
        }
    }

    override fun init() {
        CommandManager.updateCommandsAtRuntime()
        commands.clear()
        commands.addAll(
            listOf(
                WarpUtils("arachne", "arachne"),
                WarpUtils("barn", "barn"),
                WarpUtils("bayou", "bayou"),
                WarpUtils("back", "bayou"),
                WarpUtils("backwater", "bayou"),
                WarpUtils("camp", "camp"),
                WarpUtils("base", "camp"),
                WarpUtils("tunnels", "camp"),
                WarpUtils("castle", "castle"),
                WarpUtils("cn", "cn"),
                WarpUtils("nc", "cn"),
                WarpUtils("carnival", "carnival"),
                WarpUtils("crypts", "crypts"),
                WarpUtils("zombie", "crypts"),
                WarpUtils("da", "da"),
                WarpUtils("deep", "deep"),
                WarpUtils("desert", "desert"),
                WarpUtils("crystals", "crystals"),
                WarpUtils("dhub", "dhub"),
                WarpUtils("dn", "dhub"),
                WarpUtils("dh", "dhub"),
                WarpUtils("d", "dhub"),
                WarpUtils("dungeon", "dhub"),
                WarpUtils("drag", "drag"),
                WarpUtils("elizabeth", "elizabeth"),
                WarpUtils("end", "end"),
                WarpUtils("forge", "forge"),
                WarpUtils("galatea", "galatea"),
                WarpUtils("garden", "garden"),
                WarpUtils("gold", "gold"),
                WarpUtils("howl", "howl"),
                WarpUtils("wolf", "howl"),
                WarpUtils("h", "hub"),
                WarpUtils("isle", "isle"),
                WarpUtils("nether", "isle"),
                WarpUtils("jerry", "jerry"),
                WarpUtils("jungle", "jungle"),
                WarpUtils("kuudra", "kuudra"),
                WarpUtils("skull", "kuudra"),
                WarpUtils("mines", "mines"),
                WarpUtils("murk", "murk"),
                WarpUtils("murkwater", "murk"),
                WarpUtils("loch", "murk"),
                WarpUtils("museum", "museum"),
                WarpUtils("nest", "nest"),
                WarpUtils("park", "park"),
                WarpUtils("rift", "rift"),
                WarpUtils("spider", "spider"),
                WarpUtils("stonks", "stonks"),
                WarpUtils("tomb", "tomb"),
                WarpUtils("smold", "tomb"),
                WarpUtils("smoldering", "tomb"),
                WarpUtils("blaze", "tomb"),
                WarpUtils("trapper", "trapper"),
                WarpUtils("trevor", "trapper"),
                WarpUtils("taylor", "taylor"),
                WarpUtils("void", "void"),
                WarpUtils("eman", "void"),
                WarpUtils("wiz", "wizard"),
                WarpUtils("wizard", "wizard"),
            )
        )
    }

    private fun warp(destination: String) {
        if (enabled) ChatUtils.sendCommand("warp $destination")
        else return
    }

    override fun onEnable() {
        super.onEnable()
        CommandManager.updateCommandsAtRuntime()
    }

    override fun onDisable() {
        super.onDisable()
        CommandManager.updateCommandsAtRuntime()
    }
}