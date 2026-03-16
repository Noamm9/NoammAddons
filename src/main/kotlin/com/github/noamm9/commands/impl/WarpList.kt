package com.github.noamm9.commands.impl

import com.github.noamm9.commands.BaseCommand
import com.github.noamm9.commands.CommandNodeBuilder
import com.github.noamm9.features.impl.general.WarpShortcuts
import com.github.noamm9.utils.ChatUtils

object WarpList : BaseCommand("warplist") {
    override fun CommandNodeBuilder.build() {
        requires { WarpShortcuts.enabled }
        runs {
            val formatted = WarpShortcuts.warpList.joinToString("\n") { (cmd, loc) ->
                "${cmd.padEnd(WarpShortcuts.warpList.maxOf { it.first.length } + 1)}-> $loc"
            }
            ChatUtils.modMessage("&l&n&bCommand / Location\n&d$formatted")
        }
    }
}