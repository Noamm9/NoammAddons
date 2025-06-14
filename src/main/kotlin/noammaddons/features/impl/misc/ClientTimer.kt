/**
 * Noamm Addons - Timer Command
 * This file has been automagically been JSdoc'ed by https://axle.coffee using OpenAI's GPT-4.1
 * This file is part of the Noamm Addons project, which is licensed under the undefined license.
 * This code may be subject to personal license(s) used by axle.coffee or other third parties, please contact a Contributor for more information.
 *
 */
package noammaddons.features.impl.misc

import noammaddons.features.Feature

import noammaddons.ui.config.core.impl.DropdownSetting
import noammaddons.ui.config.core.impl.TextInputSetting
import noammaddons.ui.config.core.impl.ToggleSetting

/**
 * ClientTimer feature for scheduling actions like logout, quitting the game, or running a command.
 *
 * @property clientTimerMode Dropdown for selecting the timer mode ('logout', 'quit game', 'run command'). <- THIS IS A FUCKING INT FOR SOME REAOSN ITS LIKE ArRAY INT MAP WTFFF
 * @property clientTimerCommand Text input for specifying the command to run in 'run command' mode.
 * @property clientTimerCommandOnAllModes Toggle to run the command on all modes. boolean
 */
object ClientTimer: Feature("bdru me when i shizo and you shizo and you see this why are you still reading it GET OUT OF MY HEAD GET OUT OF M HEAD GET NJHOUT MF MY HEAD? NOW NOW NOW dev/ee3 lf invite can ee3 what is an ee3 he asks while i meow buti  meow wichi is a problem did i mention fuck axio axle was ratted by laskis in the big 20205 which is hobenstly quite sad now anyways i hope you;re doing very well sincei know i mam omeow meow meowm eowmekeopwm oewme") {
    public val clientTimerMode by DropdownSetting("Mode", listOf("logout", "quit game", "run command"), 0)
    public val clientTimerCommand by TextInputSetting("Command to run", "")
    public val clientTimerCommandOnAllModes by ToggleSetting("Command on all modes", false)


}
