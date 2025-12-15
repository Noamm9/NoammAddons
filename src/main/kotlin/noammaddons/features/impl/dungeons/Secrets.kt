package noammaddons.features.impl.dungeons

import gg.essential.elementa.utils.withAlpha
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.editgui.GuiElement
import noammaddons.events.*
import noammaddons.events.DungeonEvent.SecretEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.ActionBarParser.SECRETS_REGEX
import noammaddons.utils.ActionBarParser.maxSecrets
import noammaddons.utils.ActionBarParser.secrets
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.GuiUtils.currentChestName
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.PlayerUtils
import noammaddons.utils.RenderHelper.colorCodeByPresent
import noammaddons.utils.RenderHelper.getStringHeight
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.renderItem
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.favoriteColor
import noammaddons.utils.Utils.remove
import noammaddons.utils.Utils.send


object Secrets: Feature() {
    private data class ClickedSecret(val pos: BlockPos, val time: Long)
    private object SecretDisplayElement: GuiElement(hudData.getData().secretDisplay) {
        override val enabled get() = hudDisplay.value && Secrets.enabled
        var lines = listOf("&7Secrets", "&c3&7/&a7")
        val exampleLines = listOf("&7Secrets", "&c3&7/&a7")
        override val width get() = lines.maxOf { getStringWidth(it) } + 16f
        override val height get() = getStringHeight(lines)

        override fun draw() {
            val textWidth = lines.maxOf { getStringWidth(it) + 16 } * getScale() / 2
            val textHeight = lines.size * 9f * getScale() / 2

            drawCenteredText(
                lines,
                getX() + textWidth + (38 * getScale() / 4),
                getY() + textHeight,
                getScale()
            )

            if (chestIcon.value) renderItem(
                chestItem,
                getX() + textWidth - 38 * getScale() + (38 * getScale() / 4),
                getY() + textHeight - 9f * getScale(),
                getScale()
            )
        }

        override fun exampleDraw() {
            val textWidth = exampleLines.maxOf { getStringWidth(it) + 16 } * getScale() / 2
            val textHeight = exampleLines.size * 9f * getScale() / 2

            drawCenteredText(
                lines,
                getX() + textWidth + (38 * getScale() / 4),
                getY() + textHeight,
                getScale()
            )

            if (chestIcon.value) renderItem(
                chestItem,
                getX() + textWidth - 38 * getScale() + (38 * getScale() / 4),
                getY() + textHeight - 9f * getScale(),
                getScale()
            )
        }
    }

    private val hudDisplay = ToggleSetting("Secret HUD")
    private val chestIcon = ToggleSetting("Draw Chest Icon").addDependency(hudDisplay)

    private val secretClicked = ToggleSetting("Secret Clicked")
    private val displayTime = SliderSetting("Hightlight Time", 0.5, 5, 0.1, 2).addDependency(secretClicked)
    private val secretClickedColor = ColorSetting("Color", favoriteColor.withAlpha(50)).addDependency(secretClicked)
    private val mode = DropdownSetting("Mode", listOf("Fill", "Outline", "Filled Outline"), 2).addDependency(secretClicked)
    private val phase = ToggleSetting("Phase").addDependency(secretClicked)

    private val secretSound = ToggleSetting("Secret Sound")
    private val soundName = TextInputSetting("Sound Name", "random.orb").addDependency(secretSound)
    private val volume = SliderSetting("Volume", 0, 1, 0.1, 0.5).addDependency(secretSound)
    private val pitch = SliderSetting("Pitch", 0, 2, 0.1, 1.0).addDependency(secretSound)
    private val playSound = ButtonSetting("Play Sound") {
        repeat(5) {
            mc.thePlayer?.playSound(
                soundName.value,
                volume.value.toFloat(),
                pitch.value.toFloat()
            )
        }
    }.addDependency(secretSound)

    private val closeChest = ToggleSetting("Close Chest ")
    private val closeMode = DropdownSetting("Close Mode", arrayListOf("Auto", "Any Key")).addDependency(closeChest)

    override fun init() = addSettings(
        SeperatorSetting("HUD"),
        hudDisplay, chestIcon,
        SeperatorSetting("Clicked"),
        secretClicked, displayTime,
        secretClickedColor, mode, phase,
        SeperatorSetting("Sound"),
        secretSound, soundName,
        volume, pitch, playSound,
        SeperatorSetting("Close Chest"),
        closeChest, closeMode
    )

    private val clicked = mutableSetOf<ClickedSecret>()
    private var lastPlayed = System.currentTimeMillis()
    private val chestItem = ItemStack(Blocks.chest)


    @SubscribeEvent
    fun onSecret(event: SecretEvent) {
        if (secretSound.value) {
            if (event.type == SecretEvent.SecretType.ITEM && System.currentTimeMillis() - lastPlayed < 2000) return
            if (event.type == SecretEvent.SecretType.CHEST) lastPlayed = System.currentTimeMillis()
            if (clicked.any { it.pos == event.pos }) return
            playSound.defaultValue.run()
        }

        if (secretClicked.value) {
            if (clicked.any { it.pos == event.pos }) return
            clicked.add(ClickedSecret(event.pos, System.currentTimeMillis()))
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (clicked.isEmpty()) return
        clicked.removeIf { it.time + (displayTime.value.toDouble() * 1000) < System.currentTimeMillis() }
        clicked.takeUnless { it.isEmpty() }?.toList()?.forEach {
            drawBlockBox(
                it.pos,
                secretClickedColor.value,
                outline = mode.value.equalsOneOf(1, 2),
                fill = mode.value.equalsOneOf(0, 2),
                phase = phase.value
            )
        }
    }

    @SubscribeEvent
    fun onRenderOvelay(event: RenderOverlay) {
        if (! SecretDisplayElement.enabled) return
        if (! inDungeon || inBoss) return
        if (secrets == null || maxSecrets == null) return

        SecretDisplayElement.lines = listOf("&7Secrets", "${colorCodeByPresent(secrets !!, maxSecrets !!)}$secrets&7/&a$maxSecrets")
        SecretDisplayElement.draw()
    }

    @SubscribeEvent
    fun onOpenWindow(event: PacketEvent.Received) {
        if (! closeChest.value) return
        if (closeMode.value != 0) return
        if (! inDungeon) return
        val packet = event.packet as? S2DPacketOpenWindow ?: return
        if (! packet.windowTitle.noFormatText.equalsOneOf("Chest", "Large Chest")) return
        C0DPacketCloseWindow(packet.windowId).send()
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onInput(event: GuiKeybourdInputEvent) {
        if (! closeChest.value) return
        if (! inDungeon || closeMode.value != 1) return
        if (currentChestName.removeFormatting().equalsOneOf("Chest", "Large Chest")) {
            PlayerUtils.closeScreen()
        }
    }

    @SubscribeEvent
    fun onMouse(event: GuiMouseClickEvent) {
        if (! closeChest.value) return
        if (! inDungeon || closeMode.value != 1) return
        if (currentChestName.removeFormatting().equalsOneOf("Chest", "Large Chest")) {
            PlayerUtils.closeScreen()
        }
    }


    /**
     * @see noammaddons.mixins.MixinGuiIngame.modifyActionBar
     */
    @JvmStatic
    fun removeSecrets(s: String): String {
        if (! SecretDisplayElement.enabled) return s
        return s.remove(SECRETS_REGEX)
    }
}