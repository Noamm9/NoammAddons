package noammaddons.features.dungeons

import gg.essential.elementa.state.BasicState
import gg.essential.elementa.utils.withAlpha
import net.minecraft.entity.monster.EntityZombie
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.noammaddons.Companion.CHAT_PREFIX
import noammaddons.utils.ChatUtils.modMessage
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


object MimicDetector: Feature() {
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
        if (! config.sendMimicKillMessage) return
        sendPartyMessage("$CHAT_PREFIX Mimic killed!")
        modMessage("&l&cMimic killed!")
        showTitle("&cMimic Dead!")
    }


    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) = mimicKilled.set(false)


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
    fun onEntityDeath(event: LivingDeathEvent) = with(event.entity) {
        if (! check()) return@with
        if (this !is EntityZombie) return@with
        if (! this.isChild) return@with
        if (! (0 .. 3).all { this.getCurrentArmor(it) == null }) return
        sendMimicMessage()
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! inDungeon) return
        if (inBoss) return
        if (event.component.noFormatText.lowercase() !in mimicMessages) return
        mimicKilled.set(true)
    }

    @SubscribeEvent
    fun preChum(event: RenderChestEvent.Pre) {
        if (! config.highlightMimicChest) return
        if (event.chest.chestType != 1) return
        if (! check()) return
        enableChums(Color.WHITE)
    }

    @SubscribeEvent
    fun postChum(event: RenderChestEvent.Post) {
        if (! config.highlightMimicChest) return
        if (event.chest.chestType != 1) return
        if (! check()) return
        disableChums()
        RenderUtils.drawBlockBox(
            event.chest.pos,
            (if (config.disableVisualWords) Color.RED
            else Color.BLUE).withAlpha(40),
            outline = true, fill = true, phase = true
        )
    }
}