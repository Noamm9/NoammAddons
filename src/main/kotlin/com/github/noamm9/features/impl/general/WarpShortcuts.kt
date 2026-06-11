package com.github.noamm9.features.impl.general

import com.github.noamm9.commands.BaseCommand
import com.github.noamm9.commands.CommandManager
import com.github.noamm9.commands.CommandNodeBuilder
import com.github.noamm9.features.Feature
import com.github.noamm9.init.DataDownloader
import com.github.noamm9.utils.ChatUtils

object WarpShortcuts: Feature("removes the /warp in warp commands", "Warp Shortcuts") {
    override fun init() {
        DataDownloader.loadJson<Map<String, String>>("warpShortcuts.json").forEach { (command, warp) ->
            CommandManager.commands.add(object: BaseCommand(command) {
                override fun CommandNodeBuilder.build() {
                    requires { enabled }
                    runs { ChatUtils.sendCommand("warp $warp") }
                }
            })
        }
    }
}