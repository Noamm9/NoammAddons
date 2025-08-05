package noammaddons.features.impl.misc


import net.minecraft.network.play.server.S0FPacketSpawnMob
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.addRandomColorCodes
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.NumbersUtils.format
import noammaddons.utils.Utils.containsOneOf

object DamageSplash: Feature() {
    //  private val mode = DropdownSetting("Mode", arrayListOf("Minimal", "Decimal comma"))
    private val disableinBoss by ToggleSetting("Disable in Boss")

    // Thanks Skytils 😘
    val damageRegex = Regex("[✧✯]?(\\d{1,3}(?:,\\d{3})*[⚔+✧❤♞☄✷ﬗ✯]*)")

    @SubscribeEvent
    fun handleCustomDamageSplash(event: PacketEvent.Received) {
        if (! inSkyblock) return
        val packet = event.packet as? S0FPacketSpawnMob ?: return
        if (packet.entityType != 30) return // Armor stand

        // for some reason the index is inconsistent, Amazing Minecraft code
        val nameData = packet.func_149027_c()?.find { it.getObject().toString().contains("§") } ?: return
        val name = "${nameData.getObject()}".removeFormatting()
        val damage = damageRegex.matchEntire(name)?.destructured?.component1() ?: return
        if (inBoss && disableinBoss) return event.setCanceled(true)

        val newName = if (name.containsOneOf("✧", "✯")) "&f✧${addRandomColorCodes(format(damage))}&f✧"
        else "&3${format(damage)}"

        nameData.setObject(newName.addColor())
    }
}