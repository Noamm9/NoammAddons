package noammaddons.commands.commands

import kotlinx.serialization.json.*
import net.minecraft.command.ICommandSender
import noammaddons.commands.Command
import noammaddons.features.gui.ProfleViewer
import noammaddons.features.gui.ProfleViewer.getPlayerSkin
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.GuiUtils.openScreen
import noammaddons.utils.JsonUtils
import noammaddons.utils.JsonUtils.getObj
import noammaddons.utils.JsonUtils.getString

object ProfileViewerCommand: Command("pv", listOf("profileviewer"), "&cInvalid Usage. &bUsage: /pv <username>") {
    override fun processCommand(sender: ICommandSender, args: Array<out String>) {
        when (args.size) {
            1 -> getData(args[0])
            0 -> getData(mc.session.username)
            else -> return modMessage(usage)
        }
    }

    fun getData(name: String) {
        modMessage("Fetching data for $name")
        JsonUtils.get("https://sky.shiiyu.moe/api/v2/profile/$name") { obj ->
            val profiles = if (obj.containsKey("profiles")) obj["profiles"] !!.jsonObject else return@get

            val userProfile = profiles.values.first {
                it.jsonObject["current"] !!.jsonPrimitive.booleanOrNull == true
            }.jsonObject

            JsonUtils.get("https://api.icarusphantom.dev/v1/sbecommands/weight/$name") { wealth ->
                if (wealth["status"]?.jsonPrimitive?.int == 200) {
                    val data = wealth.getObj("data")
                    val userName = data?.getString("username") ?: mc.session.username
                    val rank = data?.getString("rank") ?: ""
                    val lilyWeight = data?.getObj("weight")?.getObj("lilyWeight")

                    mc.addScheduledTask {
                        if (lilyWeight != null) {
                            ProfleViewer.ProfleViewerGUI(
                                userName,
                                rank,
                                getPlayerSkin(userName),
                                userProfile, lilyWeight
                            ).let {
                                openScreen(it)
                            }
                        }
                    }
                }
            }
        }
    }
}