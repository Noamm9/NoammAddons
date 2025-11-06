package noammaddons.features.impl.general.teleport

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.events.SoundPlayEvent
import noammaddons.features.Feature
import noammaddons.features.impl.general.teleport.core.TeleportType
import noammaddons.features.impl.general.teleport.helpers.EtherwarpHelper
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.*
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.PlayerUtils.isHoldingEtherwarpItem
import noammaddons.utils.PlayerUtils.rightClick
import noammaddons.utils.PlayerUtils.swingHand
import noammaddons.utils.PlayerUtils.toggleSneak
import noammaddons.utils.Utils.equalsOneOf


object Etherwarp: Feature("Various features for the Etherwarp Ability") {
    private val leftClickEtherwarp = ToggleSetting("Left Click Etherwarp ", true)
    private val swingHandToggle = ToggleSetting("Swing Hand", true).addDependency(leftClickEtherwarp)
    private val autoSneakToggle = ToggleSetting("Auto Sneak", false).addDependency(leftClickEtherwarp)
    private val autoSneakDelaySlider = SliderSetting("Delay", 50, 150, 1, 50).addDependency(autoSneakToggle).addDependency(leftClickEtherwarp)

    private val etherwarpSound = ToggleSetting("Etherwarp Sound ")
    private val soundName = TextInputSetting("Sound Name", "random.orb").addDependency(etherwarpSound)
    private val volume = SliderSetting("Volume", 0, 1, 0.1, 0.5).addDependency(etherwarpSound)
    private val pitch = SliderSetting("Pitch", 0, 2, 0.1, 1.0).addDependency(etherwarpSound)
    private val playSound = ButtonSetting("Play Sound") {
        mc.addScheduledTask {
            repeat(5) {
                SoundUtils.playSound(
                    soundName.value,
                    volume.value.toFloat(),
                    pitch.value.toFloat()
                )
            }
        }
    }.addDependency(etherwarpSound)
    private val zeroPingSound = ToggleSetting("Zero Ping Sound").addDependency(etherwarpSound)

    override fun init() = addSettings(
        SeperatorSetting("Left Click Etherwarp"),
        leftClickEtherwarp, swingHandToggle, autoSneakToggle, autoSneakDelaySlider,
        SeperatorSetting("Etherwarp Sound"),
        etherwarpSound, soundName, volume, pitch, playSound, zeroPingSound
    )

    @SubscribeEvent
    fun onSound(event: SoundPlayEvent) {
        if (! etherwarpSound.value) return
        if (event.name != "mob.enderdragon.hit") return
        if (event.pitch != 0.53968257f) return
        if (! zeroPingSound.value) playSound.defaultValue.run()
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onC08PacketPlayerBlockPlacement(event: PacketEvent.Sent) {
        if (! etherwarpSound.value) return
        if (! zeroPingSound.value) return
        val packet = event.packet as? C08PacketPlayerBlockPlacement ?: return
        if (packet.placedBlockDirection != 255) return
        if (LocationUtils.dungeonFloorNumber == 7 && LocationUtils.inBoss) return
        if (ActionBarParser.currentMana < ActionBarParser.maxMana * 0.1) return
        if (ScanUtils.currentRoom?.data?.name.equalsOneOf("New Trap", "Old Trap", "Teleport Maze", "Boulder")) return
        runCatching { if ((mc.objectMouseOver.blockPos?.let { getBlockAt(it).getBlockId() } ?: 0) in setOf(146, 54, 130, 154, 118, 69, 77, 143, 96, 167)) return }
        if (LocationUtils.isInHubCarnival()) return

        val tpInfo = TeleportOverlay.getType(packet.stack)?.takeIf { it.type == TeleportType.Etherwarp } ?: return
        val playerPos = ServerPlayer.player.getVec() ?: return
        val playerRot = ServerPlayer.player.getRotation() ?: return
        val etherPos = EtherwarpHelper.getEtherPos(playerPos, playerRot, tpInfo.distance).takeIf { it.succeeded && it.pos != null } ?: return

        if (ScanUtils.getRoomFromPos(etherPos.pos !!)?.data?.name.equalsOneOf("Teleport Maze", "Boulder")) return
        playSound.defaultValue.run()
    }

    @SubscribeEvent
    fun onLeftClick(event: MouseEvent) {
        if (! leftClickEtherwarp.value) return
        if (! event.buttonstate) return
        if (event.button != 0) return
        if (! ServerPlayer.player.sneaking && ! autoSneakToggle.value) return
        val item = ServerPlayer.player.getHeldItem() ?: return
        if (! isHoldingEtherwarpItem(item)) return
        event.isCanceled = true

        scope.launch {
            if (autoSneakToggle.value && ! ServerPlayer.player.sneaking) {
                val halfTime = autoSneakDelaySlider.value.toLong() / 2
                toggleSneak(true)
                delay(halfTime)
                if (swingHandToggle.value) swingHand()
                rightClick()
                delay(halfTime)
                toggleSneak(false)
            }
            else {
                if (swingHandToggle.value) swingHand()
                rightClick()
            }
        }
    }
}