package noammaddons.features.impl.misc

import noammaddons.commands.CommandManager
import noammaddons.commands.commands.TimerCommand
import noammaddons.features.Feature
import noammaddons.ui.config.core.annotations.AlwaysActive
import noammaddons.ui.config.core.impl.*

@AlwaysActive
object ClientTimer: Feature("Configuration for the /timer command.") {
    val clientTimerMode by DropdownSetting("Mode", listOf("Disconnect", "Quit Game", "Run Command"))
    val clientTimerCommandOnAllModes by ToggleSetting("Always Run Command").addDependency { clientTimerMode != 2 }
    val clientTimerCommand by TextInputSetting("Command to run", "").addDependency { clientTimerMode != 2 && ! clientTimerCommandOnAllModes }

    override fun onDisable() {
        CommandManager.unregisterCommand(TimerCommand)
    }

    override fun onEnable() {
        CommandManager.registerCommand(TimerCommand)
    }
}