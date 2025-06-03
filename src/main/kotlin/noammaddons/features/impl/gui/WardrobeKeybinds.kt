package noammaddons.features.impl.gui

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.GuiKeybourdInputEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.*
import noammaddons.utils.Utils.equalsOneOf
import org.lwjgl.input.Keyboard

object WardrobeKeybinds: Feature("Allows you to use your hotbar binds to swap armors in your wardrobe") {
    private val keyMap = mapOf(
        0 to 36, 1 to 37, 2 to 38, 3 to 39, 4 to 40,
        5 to 41, 6 to 42, 7 to 43, 8 to 44
    )

    private val closeAfterUse = ToggleSetting("Auto Close On Use")
    private val useHotbarBinds = ToggleSetting("Use Hotbar Binds")
    private val keybinds = listOf(
        KeybindSetting("Wardrobe Slot 1", Keyboard.KEY_1),
        KeybindSetting("Wardrobe Slot 2", Keyboard.KEY_2),
        KeybindSetting("Wardrobe Slot 3", Keyboard.KEY_3),
        KeybindSetting("Wardrobe Slot 4", Keyboard.KEY_4),
        KeybindSetting("Wardrobe Slot 5", Keyboard.KEY_5),
        KeybindSetting("Wardrobe Slot 6", Keyboard.KEY_6),
        KeybindSetting("Wardrobe Slot 7", Keyboard.KEY_7),
        KeybindSetting("Wardrobe Slot 8", Keyboard.KEY_8),
        KeybindSetting("Wardrobe Slot 9", Keyboard.KEY_9)
    ).onEach { it.addDependency { useHotbarBinds.value } }


    private lateinit var hotbarKeyMap: Map<Int, Int>
    private var lastClick = System.currentTimeMillis()

    override fun init() {
        hotbarKeyMap = mc.gameSettings.keyBindsHotbar.mapIndexed { i, key -> key.keyCode to i }.toMap()

        addSettings(
            closeAfterUse, useHotbarBinds,
            SeperatorSetting("Keybinds").addDependency { useHotbarBinds.value },
            *keybinds.toTypedArray()
        )
    }

    @SubscribeEvent
    fun onKeyInput(event: GuiKeybourdInputEvent) {
        if (! ActionUtils.inWardrobeMenu) return
        if (System.currentTimeMillis() - lastClick < 300) return
        if (event.keyCode.equalsOneOf(Keyboard.KEY_ESCAPE, Keyboard.KEY_E)) return
        val windowId = mc.thePlayer?.openContainer?.windowId ?: return
        val index = if (useHotbarBinds.value) hotbarKeyMap[event.keyCode] ?: return
        else keybinds.withIndex().find { (_, key) -> key.value == event.keyCode }?.index ?: return
        val slot = keyMap[index]?.takeIf { mc.thePlayer.openContainer.getSlot(it).stack != null } ?: return
        event.isCanceled = true

        if (closeAfterUse.value) {
            GuiUtils.sendWindowClickPacket(slot, 0, 0)
            PlayerUtils.closeScreen()
        }
        else mc.playerController.windowClick(windowId, slot, 0, 0, mc.thePlayer)

        lastClick = System.currentTimeMillis()
    }
}