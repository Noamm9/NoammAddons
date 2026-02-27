package com.github.noamm9.features.impl.misc

import com.github.noamm9.commands.BaseCommand
import com.github.noamm9.commands.CommandNodeBuilder
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils

object WarpShortcuts : Feature("removes the /warp in warp commands", "Warp Shortcuts") {

    class WarpUtils(var input: String, var output: String) : BaseCommand(input) {
        override fun CommandNodeBuilder.build() {
            requires { WarpShortcuts.enabled }
            runs {
                warp(output)
            }
        }
    }

    val commands = mutableListOf<WarpUtils>()
    override fun init() {
        commands.clear()
        commands.addAll(
            listOf(
                WarpUtils("arachne", "arachne"),
                WarpUtils("barn", "barn"),
                WarpUtils("bayou", "bayou"),
                WarpUtils("galatea", "galatea"),
                WarpUtils("back", "bayou"),
                WarpUtils("backwater", "bayou"),
                WarpUtils("camp", "camp"),
                WarpUtils("castle", "castle"),
                WarpUtils("crypts", "crypts"),
                WarpUtils("zombie", "crypts"),
                WarpUtils("da", "da"),
                WarpUtils("deep", "deep"),
                WarpUtils("desert", "desert"),
                WarpUtils("drag", "drag"),
                WarpUtils("end", "end"),
                WarpUtils("forge", "forge"),
                WarpUtils("gold", "gold"),
                WarpUtils("howl", "howl"),
                WarpUtils("wolf", "howl"),
                WarpUtils("isle", "isle"),
                WarpUtils("jungle", "jungle"),
                WarpUtils("kuudra", "kuudra"),
                WarpUtils("skull", "kuudra"),
                WarpUtils("mines", "mines"),
                WarpUtils("murk", "murk"),
                WarpUtils("murkwater", "murk"),
                WarpUtils("museum", "museum"),
                WarpUtils("nest", "nest"),
                WarpUtils("cn", "cn"),
                WarpUtils("nc", "cn"),
                WarpUtils("park", "park"),
                WarpUtils("rift", "rift"),
                WarpUtils("spider", "spider"),
                WarpUtils("stonks", "stonks"),
                WarpUtils("tomb", "tomb"),
                WarpUtils("smold", "tomb"),
                WarpUtils("blaze", "tomb"),
                WarpUtils("trapper", "trapper"),
                WarpUtils("void", "void"),
                WarpUtils("eman", "void"),
                WarpUtils("wiz", "wizard"),
                WarpUtils("dhub", "dhub"),
                WarpUtils("dn", "dhub"),
                WarpUtils("dh", "dhub"),
                WarpUtils("d", "dhub"),
                WarpUtils("dungeon", "dhub"),
                WarpUtils("h", "hub")
            )
        )
    }

    private fun warp(destination: String) {
        if (enabled) ChatUtils.sendCommand("warp $destination")
        else return
    }
}