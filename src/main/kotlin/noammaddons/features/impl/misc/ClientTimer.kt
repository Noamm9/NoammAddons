package noammaddons.features.impl.misc

import noammaddons.commands.CommandManager
import noammaddons.commands.commands.TimerCommand
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*

object ClientTimer: Feature("Configuration for the /timer command. Allows scheduling actions like logout, quitting the game, or running a command", "Timer Command") {
    val mode by DropdownSetting("Mode", listOf("Disconnect", "Quit Game", "Run Command"))
    val cmdAlways by ToggleSetting("Always Run Command").hideIf { mode == 2 }
    val cmd by TextInputSetting("Command to run", "").hideIf { mode != 2 && ! cmdAlways }

    override fun onEnable() {
        CommandManager.registerCommand(TimerCommand)
    }

    override fun onDisable() {
        CommandManager.unregisterCommand(TimerCommand)
    }
}