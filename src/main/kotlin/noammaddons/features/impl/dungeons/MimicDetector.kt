package noammaddons.features.impl.dungeons

import gg.essential.elementa.state.BasicState
import gg.essential.elementa.utils.withAlpha
import net.minecraft.entity.monster.EntityZombie
import net.minecraft.tileentity.TileEntityChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.ui.config.core.annotations.AlwaysActive
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.ItemUtils.getSkullValue
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.RenderHelper.disableChums
import noammaddons.utils.RenderHelper.enableChums
import noammaddons.utils.RenderUtils
import java.awt.Color

@AlwaysActive
object MimicDetector: Feature("Detects when a Mimic or Prince is killed") {
    private val mimic = ToggleSetting("Send Mimic Message", true)
    private val prince = ToggleSetting("Send Prince Message", true)
    private val highlightChest = ToggleSetting("Highlight Chest", false)
    private val highlightColor = ColorSetting("Highlight Color", Color.RED.withAlpha(0.3f))
        .addDependency(highlightChest)

    override fun init() = addSettings(
        mimic, prince,
        SeperatorSetting("Highlight"),
        highlightChest, highlightColor
    )

    private val isMimicDetectionActive get() = ! mimicKilled.get() && inDungeon && (dungeonFloorNumber ?: 0) >= 6 && ! inBoss
    val mimicKilled = BasicState(false)
    val princeKilled = BasicState(false)


    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        mimicKilled.set(false)
        princeKilled.set(false)
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! inDungeon) return
        val msg = event.component.noFormatText.lowercase()
        handlePrinceKillChat(msg)
        handleMimicKillChat(msg)
    }

    @SubscribeEvent
    fun onEntityLeaveWorld(event: EntityDeathEvent) {
        if (! isMimicDetectionActive) return
        val entity = event.entity as? EntityZombie ?: return
        if (! entity.isChild) return
        if (getSkullValue(entity) != MIMIC_TEXTURE) return
        confirmMimicKill()
    }

    @SubscribeEvent
    fun onRenderChestPre(event: RenderChestEvent.Pre) {
        if (shouldHighlightMimicChest(event.chest))
            enableChums()
    }

    @SubscribeEvent
    fun onRenderChestPost(event: RenderChestEvent.Post) {
        if (! shouldHighlightMimicChest(event.chest)) return
        disableChums()
        RenderUtils.drawBlockBox(
            event.chest.pos,
            highlightColor.value,
            outline = true, fill = true, phase = true
        )
    }

    private fun handlePrinceKillChat(message: String) {
        if (princeMessages.any { message.contains(it) }) {
            princeKilled.set(true)
            if (enabled && prince.value && message == "a prince falls. +1 bonus score") {
                sendPartyMessage("Prince Killed")
            }
        }
    }

    private fun handleMimicKillChat(message: String) {
        if (isMimicDetectionActive && mimicMessages.any { message.contains(it) }) {
            mimicKilled.set(true)
        }
    }

    private fun confirmMimicKill() {
        if (mimicKilled.get()) return
        mimicKilled.set(true)

        if (enabled && mimic.value)
            sendPartyMessage("Mimic killed!")
    }

    private fun shouldHighlightMimicChest(chest: TileEntityChest): Boolean {
        return enabled &&
                highlightChest.value &&
                isMimicDetectionActive &&
                chest.chestType == 1 // Trapped Chest
    }

    private const val MIMIC_TEXTURE =
        "ewogICJ0aW1lc3RhbXAiIDogMTY3Mjc2NTM1NTU0MCwKICAicHJvZmlsZUlkIiA6ICJhNWVmNzE3YWI0MjA0MTQ4ODlhOTI5ZDA5OTA0MzcwMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJXaW5zdHJlYWtlcnoiLAogICJzaWduYXR1cmVSZXF1aWJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTE5YzEyNTQzYmM3NzkyNjA1ZWY2OGUxZjg3NDlhZThmMmEzODFkOTA4NWQ0ZDRiNzgwYmExMjgyZDM1OTdhMCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"

    private val mimicMessages = setOf(
        "mimic dead!", "mimic dead", "mimic killed!", "mimic killed",
        "\$skytils-dungeon-score-mimic$", "child destroyed!", "mimic obliterated!",
        "mimic exorcised!", "mimic destroyed!", "mimic annhilated!",
        "breefing killed", "breefing dead"
    )

    private val princeMessages = setOf(
        "prince dead", "prince dead!", "\$skytils-dungeon-score-prince\$",
        "prince killed", "prince slain", "prince killed!",
        "a prince falls. +1 bonus score"
    )
}