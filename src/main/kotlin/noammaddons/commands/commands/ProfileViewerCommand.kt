package noammaddons.commands.commands

import kotlinx.serialization.json.*
import net.minecraft.command.ICommandSender
import noammaddons.commands.Command
import noammaddons.features.gui.ProfileViewer
import noammaddons.features.gui.ProfileViewer.createFakePlayer
import noammaddons.features.gui.ProfileViewer.data
import noammaddons.features.gui.ProfileViewer.profileCache
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
        openScreen(ProfileViewer.ProfleViewerGUI())

        profileCache[name.lowercase()]?.run {
            if (System.currentTimeMillis() - second < 120_000) {
                data.name = first.name
                data.rank = first.rank
                data.entityOtherPlayerMP = first.entityOtherPlayerMP
                data.skyCryptData = first.skyCryptData
                data.lilyWeight = first.lilyWeight
                return
            }
        }


        JsonUtils.get("https://sky.shiiyu.moe/api/v2/profile/$name") { obj ->
            val profiles = if (obj.containsKey("profiles")) obj["profiles"] !!.jsonObject else return@get

            data.skyCryptData = profiles.values.first {
                it.jsonObject["current"] !!.jsonPrimitive.booleanOrNull == true
            }.jsonObject
        }

        JsonUtils.get("https://api.icarusphantom.dev/v1/sbecommands/weight/$name") { wealth ->
            if (wealth["status"]?.jsonPrimitive?.int != 200) return@get
            val data_ = wealth.getObj("data")
            val userName = data_?.getString("username") ?: ""
            val rank = data_?.getString("rank") ?: ""
            val lilyWeight = data_?.getObj("weight")?.getObj("lilyWeight")

            data.name = userName
            data.rank = rank
            data.lilyWeight = lilyWeight
        }

        data.entityOtherPlayerMP = createFakePlayer(name)

        profileCache[name.lowercase()] = data to System.currentTimeMillis()
    }
}