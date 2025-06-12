package noammaddons.features.impl.dungeons

import gg.essential.elementa.state.BasicState
import gg.essential.elementa.utils.withAlpha
import net.minecraft.entity.monster.EntityZombie
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.noammaddons.Companion.CHAT_PREFIX
import noammaddons.ui.config.core.annotations.AlwaysActive
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.ItemUtils.getSkullValue
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.RenderHelper.disableChums
import noammaddons.utils.RenderHelper.enableChums
import noammaddons.utils.RenderUtils
import java.awt.Color

@AlwaysActive
object MimicDetector: Feature("Detects when a mimic is killed") {
    private val sendMessage = ToggleSetting("Send Message", true)
    private val message = TextInputSetting("Message", "Mimic killed!").addDependency(sendMessage)
    private val showTitle = ToggleSetting("Show Title", false)
    private val titleMsg = TextInputSetting("Title Message", "Mimic killed!").addDependency(showTitle)
    private val highlightChest = ToggleSetting("Highlight Chest", false)
    private val highlightColor = ColorSetting("Highlight Color", Color.RED.withAlpha(0.3f)).addDependency(highlightChest)

    val mimicKilled = BasicState(false)
    fun check() = ! mimicKilled.get() && inDungeon && (dungeonFloorNumber ?: 0) >= 6 && ! inBoss

    private const val MIMIC_TEXTURE =
        "ewogICJ0aW1lc3RhbXAiIDogMTY3Mjc2NTM1NTU0MCwKICAicHJvZmlsZUlkIiA6ICJhNWVmNzE3YWI0MjA0MTQ4ODlhOTI5ZDA5OTA0MzcwMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJXaW5zdHJlYWtlcnoiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTE5YzEyNTQzYmM3NzkyNjA1ZWY2OGUxZjg3NDlhZThmMmEzODFkOTA4NWQ0ZDRiNzgwYmExMjgyZDM1OTdhMCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"

    private val mimicMessages = listOf(
        "mimic dead!",
        "mimic dead",
        "mimic killed!",
        "mimic killed",
        "\$skytils-dungeon-score-mimic$",
        "child destroyed!",
        "mimic obliterated!",
        "mimic exorcised!",
        "mimic destroyed!",
        "mimic annhilated!",
        "breefing killed",
        "breefing dead"
    )

    private fun sendMimicMessage() {
        mimicKilled.set(true)
        if (! enabled) return
        if (sendMessage.value) sendPartyMessage("$CHAT_PREFIX ${message.value}")
        if (showTitle.value) showTitle(titleMsg.value)
    }

    override fun init() = addSettings(
        sendMessage, message,
        showTitle, titleMsg,
        SeperatorSetting("Highlight"),
        highlightChest, highlightColor
    )


    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) = mimicKilled.set(false)

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! inDungeon) return
        if (event.component.noFormatText.lowercase() !in mimicMessages) return
        mimicKilled.set(true)
    }

    @SubscribeEvent
    fun onEntityLeaveWorld(event: EntityLeaveWorldEvent) {
        if (! check()) return
        if (event.entity !is EntityZombie) return
        if (! event.entity.isChild) return
        if (event.entity.isEntityAlive) return
        if (getSkullValue(event.entity) != MIMIC_TEXTURE) return
        sendMimicMessage()
    }

    @SubscribeEvent
    fun preChum(event: RenderChestEvent.Pre) {
        if (! highlightChest.value) return
        if (event.chest.chestType != 1) return
        if (! check()) return
        enableChums()
    }

    @SubscribeEvent
    fun postChum(event: RenderChestEvent.Post) {
        if (! highlightChest.value) return
        if (event.chest.chestType != 1) return
        if (! check()) return

        disableChums()

        RenderUtils.drawBlockBox(
            event.chest.pos,
            highlightColor.value,
            outline = true, fill = true, phase = true
        )
    }
}