package com.github.noamm9.ui.gui

//#if CHEAT

import com.github.noamm9.features.impl.general.AutoHotbar.Profile
import com.github.noamm9.features.impl.general.AutoHotbar.SwapRule
import com.github.noamm9.features.impl.general.AutoHotbar.config
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.utils.componnents.UIButton
import com.github.noamm9.ui.utils.componnents.UISearchBox
import com.github.noamm9.utils.items.ItemUtils.itemUUID
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.render.ItemRenderer
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import java.awt.Color

class AutoHotbarScreen: Screen(Component.literal("AutoSwap Configuration")) {
    private var selectedTriggerIndex = 0
    private inline val triggers get() = config.activeTriggers.keys.toList()

    private var isCreatingNew = false
    private var isCreatingProfile = false
    private var isTriggerDropdownOpen = false
    private var isProfileDropdownOpen = false
    private var editingTrigger: String? = null
    private var editEnabled = true

    private lateinit var nameInput: UISearchBox
    private lateinit var messageInput: UISearchBox
    private lateinit var saveTriggerBtn: UIButton
    private lateinit var cancelTriggerBtn: UIButton
    private lateinit var enabledToggleBtn: UIButton
    private lateinit var selectorBtn: UIButton
    private lateinit var createButton: UIButton
    private lateinit var deleteBtn: UIButton

    private lateinit var profileSelectorBtn: UIButton
    private lateinit var createProfileBtn: UIButton
    private lateinit var deleteProfileBtn: UIButton
    private lateinit var profileNameInput: UISearchBox
    private lateinit var saveProfileBtn: UIButton
    private lateinit var cancelProfileBtn: UIButton

    private var sourceItem: ItemStack? = null
    private var sourceSlot: Int = - 1

    override fun init() {
        super.init()
        val centerX = width / 2
        val centerY = height / 2

        profileSelectorBtn = UIButton(centerX - 75, centerY - 98, 110, 20, config.activeProfile) {
            if (config.profiles.isEmpty()) return@UIButton
            isProfileDropdownOpen = ! isProfileDropdownOpen
            isTriggerDropdownOpen = false
        }

        createProfileBtn = UIButton(centerX + 38, centerY - 98, 20, 20, "§a+") {
            isProfileDropdownOpen = false
            isTriggerDropdownOpen = false
            isCreatingProfile = true
            isCreatingNew = false
            profileNameInput.value = ""
            refreshVisibility()
        }

        deleteProfileBtn = UIButton(centerX + 61, centerY - 98, 20, 20, "§c§lX") {
            isProfileDropdownOpen = false
            isTriggerDropdownOpen = false
            if (config.profiles.size <= 1) return@UIButton
            val toDelete = config.activeProfile
            val remainingProfiles = config.profiles.keys.filter { it != toDelete }
            if (remainingProfiles.isNotEmpty()) {
                config.profiles.remove(toDelete)
                config.activeProfile = remainingProfiles.first()
                selectedTriggerIndex = 0
                profileSelectorBtn.message = Component.literal(config.activeProfile)
                selectorBtn.message = Component.literal(getCurrentTriggerLabel())
            }
        }

        selectorBtn = UIButton(centerX - 75, centerY - 75, 110, 20, getCurrentTriggerLabel()) {
            if (triggers.isEmpty()) return@UIButton
            isTriggerDropdownOpen = ! isTriggerDropdownOpen
            isProfileDropdownOpen = false
        }

        createButton = UIButton(centerX + 38, centerY - 75, 20, 20, "§a+") {
            isTriggerDropdownOpen = false
            isProfileDropdownOpen = false
            if (isCreatingNew && editingTrigger == null) closeEditor()
            else {
                isCreatingNew = true
                isCreatingProfile = false
                editingTrigger = null
                editEnabled = true
                sourceItem = null
                sourceSlot = - 1
                nameInput.value = ""
                messageInput.value = ""
                updateEnabledButton()
                refreshVisibility()
            }
        }

        deleteBtn = UIButton(centerX + 61, centerY - 75, 20, 20, "§c§lX") {
            if (triggers.isEmpty()) return@UIButton
            val trigger = getCurrentTrigger()
            config.activeTriggers.remove(trigger)
            config.activeRules.remove(trigger)
            config.activeDisabled.remove(trigger)
            selectedTriggerIndex = 0
            selectorBtn.message = Component.literal(getCurrentTriggerLabel())
            isTriggerDropdownOpen = false
        }

        nameInput = UISearchBox(centerX - 80, centerY - 25, 160, 20, Component.literal("Name"))
        messageInput = UISearchBox(centerX - 80, centerY + 5, 160, 20, Component.literal("Msg"))
        messageInput.setMaxLength(256)

        saveTriggerBtn = UIButton(centerX - 80, centerY + 35, 78, 20, "§aSave") {
            val name = nameInput.value.trim()
            val message = messageInput.value.trim()
            if (name.isBlank() || message.isBlank()) return@UIButton

            val previous = editingTrigger
            if (previous == null) {
                config.activeTriggers[name] = message
                if (editEnabled) config.activeDisabled.remove(name) else config.activeDisabled.add(name)
            }
            else {
                if (previous != name) {
                    val oldRules = config.activeRules.remove(previous)
                    config.activeTriggers.remove(previous)
                    config.activeTriggers[name] = message
                    if (oldRules != null) config.activeRules[name] = oldRules
                    config.activeDisabled.remove(previous)
                }
                else {
                    config.activeTriggers[previous] = message
                }
                if (editEnabled) config.activeDisabled.remove(name) else config.activeDisabled.add(name)
            }

            selectedTriggerIndex = triggers.indexOf(name).coerceAtLeast(0)
            selectorBtn.message = Component.literal(getCurrentTriggerLabel())
            closeEditor()
        }

        cancelTriggerBtn = UIButton(centerX + 2, centerY + 35, 78, 20, "§cCancel") {
            closeEditor()
        }

        enabledToggleBtn = UIButton(centerX - 80, centerY + 60, 160, 20, "",
            { if (editEnabled) Color.GREEN else Color.RED }
        ) {
            editEnabled = ! editEnabled
            updateEnabledButton()
        }

        profileNameInput = UISearchBox(centerX - 80, centerY - 25, 160, 20, Component.literal("Profile Name"))

        saveProfileBtn = UIButton(centerX - 80, centerY + 10, 78, 20, "§aSave") {
            val name = profileNameInput.value.trim()
            if (name.isBlank()) return@UIButton
            if (! config.profiles.containsKey(name)) {
                config.profiles[name] = Profile()
            }
            config.activeProfile = name
            selectedTriggerIndex = 0
            profileSelectorBtn.message = Component.literal(name)
            selectorBtn.message = Component.literal(getCurrentTriggerLabel())
            closeProfileEditor()
        }

        cancelProfileBtn = UIButton(centerX + 2, centerY + 10, 78, 20, "§cCancel") {
            closeProfileEditor()
        }

        addRenderableWidget(profileSelectorBtn)
        addRenderableWidget(createProfileBtn)
        addRenderableWidget(deleteProfileBtn)
        addRenderableWidget(profileNameInput)
        addRenderableWidget(saveProfileBtn)
        addRenderableWidget(cancelProfileBtn)

        addRenderableWidget(selectorBtn)
        addRenderableWidget(createButton)
        addRenderableWidget(deleteBtn)
        addRenderableWidget(nameInput)
        addRenderableWidget(messageInput)
        addRenderableWidget(saveTriggerBtn)
        addRenderableWidget(cancelTriggerBtn)
        addRenderableWidget(enabledToggleBtn)

        updateEnabledButton()
        refreshVisibility()
    }

    override fun render(ctx: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val centerX = width / 2
        val centerY = height / 2

        Render2D.drawRect(ctx, centerX - 110f, centerY - 105f, 220f, 190f, Color(15, 15, 15, 200))

        if (isCreatingProfile) {
            Render2D.drawRect(ctx, centerX - 90f, centerY - 45f, 180f, 130f, Color(25, 25, 25, 255))
            Render2D.drawCenteredString(ctx, "New Profile", centerX, centerY - 40, Color.GREEN)

            if (profileNameInput.value.isBlank()) Render2D.drawString(ctx, "Profile Name", centerX - 75, centerY - 20, Color.GRAY)
        }
        else if (isCreatingNew) {
            Render2D.drawRect(ctx, centerX - 90f, centerY - 45f, 180f, 130f, Color(25, 25, 25, 255))
            val title = if (editingTrigger == null) "New Trigger" else "Edit Trigger"
            Render2D.drawCenteredString(ctx, title, centerX, centerY - 40, Color.GREEN)

            if (nameInput.value.isBlank()) Render2D.drawString(ctx, "Name", centerX - 75, centerY - 20, Color.GRAY)
            if (messageInput.value.isBlank()) Render2D.drawString(ctx, "Message", centerX - 75, centerY + 10, Color.GRAY)
        }
        else {
            if (triggers.isEmpty()) {
                Render2D.drawCenteredString(ctx, "No triggers! Click [+] to start.", centerX, centerY, Color.GRAY)
            }
            else {
                Render2D.drawCenteredString(ctx, "Select item, then hotbar slot", centerX, centerY - 50, Color.GRAY)
                renderInventory(ctx, centerX, centerY, mouseX, mouseY)
                ItemRenderer.endItemRendererBatch(ctx)
            }
        }

        super.render(ctx, mouseX, mouseY, partialTick)

        if (isProfileDropdownOpen) drawProfileDropdown(ctx, centerX - 75, centerY - 78, 110, mouseX, mouseY)
        if (isTriggerDropdownOpen) drawTriggerDropdown(ctx, centerX - 75, centerY - 55, 110, mouseX, mouseY)
    }

    private fun drawProfileDropdown(ctx: GuiGraphics, x: Int, startY: Int, boxWidth: Int, mx: Int, my: Int) {
        val profilesList = config.profiles.keys.toList()
        val itemHeight = 16
        val listHeight = profilesList.size * itemHeight
        Render2D.drawRect(ctx, x.toFloat(), startY.toFloat(), boxWidth.toFloat(), listHeight.toFloat(), Style.accentColor)
        Render2D.drawRect(ctx, x.toFloat(), startY.toFloat(), boxWidth.toFloat(), listHeight.toFloat(), Color(25, 25, 25, 255))

        profilesList.forEachIndexed { index, profileName ->
            val itemY = startY + (index * itemHeight)
            val isHovered = mx >= x && mx <= x + boxWidth && my >= itemY && my <= itemY + itemHeight

            if (isHovered) {
                Render2D.drawRect(ctx, x.toFloat(), itemY.toFloat(), boxWidth.toFloat(), itemHeight.toFloat(), Color(255, 255, 255, 40))
            }

            val textColor = if (profileName == config.activeProfile) Style.accentColor else Color.WHITE
            Render2D.drawString(ctx, profileName, x + 5, itemY + 4, textColor)
        }
    }

    private fun drawTriggerDropdown(ctx: GuiGraphics, x: Int, startY: Int, boxWidth: Int, mx: Int, my: Int) {
        val itemHeight = 16
        val listHeight = triggers.size * itemHeight
        Render2D.drawRect(ctx, x.toFloat(), startY.toFloat(), boxWidth.toFloat(), listHeight.toFloat(), Style.accentColor)
        Render2D.drawRect(ctx, x.toFloat(), startY.toFloat(), boxWidth.toFloat(), listHeight.toFloat(), Color(25, 25, 25, 255))

        triggers.forEachIndexed { index, triggerName ->
            val itemY = startY + (index * itemHeight)
            val isHovered = mx >= x && mx <= x + boxWidth && my >= itemY && my <= itemY + itemHeight

            if (isHovered) {
                Render2D.drawRect(ctx, x.toFloat(), itemY.toFloat(), boxWidth.toFloat(), itemHeight.toFloat(), Color(255, 255, 255, 40))
            }

            val isDisabled = triggerName in config.activeDisabled
            val displayName = if (isDisabled) "$triggerName (off)" else triggerName
            val textColor = when {
                index == selectedTriggerIndex && ! isDisabled -> Style.accentColor
                index == selectedTriggerIndex && isDisabled -> Color(200, 200, 200)
                isDisabled -> Color.GRAY
                else -> Color.WHITE
            }
            Render2D.drawString(ctx, displayName, x + 5, itemY + 4, textColor)
        }
    }

    private fun renderInventory(ctx: GuiGraphics, cx: Int, cy: Int, mouseX: Int, mouseY: Int) {
        val inv = minecraft?.player?.inventory ?: return

        ctx.backgroundPass(cx, cy, mouseX, mouseY, inv)
        ItemRenderer.endItemRendererBatch(ctx)
        ctx.forgroundPass(cx, cy, mouseX, mouseY, inv)
    }

    private fun GuiGraphics.backgroundPass(cx: Int, cy: Int, mouseX: Int, mouseY: Int, inv: Inventory) {
        for (i in 0 until 27) {
            val x = cx - 82 + ((i % 9) * 18)
            val y = cy - 20 + ((i / 9) * 18)
            val stack = inv.getItem(i + 9)
            val swapRule = config.activeRules[getCurrentTrigger()]?.find { rule ->
                stack.itemUUID.ifBlank { stack.skyblockId }.ifBlank { stack.hoverName.string } == rule.id
            }

            val isHovered = mouseX >= x && mouseX <= x + 18 && mouseY >= y && mouseY <= y + 18
            val color = when {
                isHovered -> Color(255, 255, 255, 60)
                swapRule != null -> Color(0, 255, 255, 40)
                else -> Color(255, 255, 255, 20)
            }

            Render2D.drawRect(this, x.toFloat(), y.toFloat(), 18f, 18f, color)
            if (! stack.isEmpty) ItemRenderer.drawBatchedItemStack(this, stack, x + 1, y + 1)
        }

        for (i in 0 until 9) {
            val x = cx - 82 + (i * 18)
            val y = cy + 45
            val stack = inv.getItem(i)
            val rule = config.activeRules[getCurrentTrigger()]?.find { it.hotbarSlot == i }

            val isHovered = mouseX >= x && mouseX <= x + 18 && mouseY >= y && mouseY <= y + 18
            val color = when {
                isHovered -> Color(255, 255, 255, 60)
                rule != null -> Color(0, 255, 255, 40)
                else -> Color(255, 255, 255, 20)
            }

            Render2D.drawRect(this, x.toFloat(), y.toFloat(), 18f, 18f, color)
            if (! stack.isEmpty) ItemRenderer.drawBatchedItemStack(this, stack, x + 1, y + 1)
        }
    }

    private fun GuiGraphics.forgroundPass(cx: Int, cy: Int, mouseX: Int, mouseY: Int, inv: Inventory) {
        for (i in 0 until 27) {
            val x = cx - 82 + ((i % 9) * 18)
            val y = cy - 20 + ((i / 9) * 18)
            val stack = inv.getItem(i + 9)
            val swapRule = config.activeRules[getCurrentTrigger()]?.find { rule ->
                stack.itemUUID.ifBlank { stack.skyblockId }.ifBlank { stack.hoverName.string } == rule.id
            }

            val isHovered = mouseX >= x && mouseX <= x + 18 && mouseY >= y && mouseY <= y + 18

            if (! stack.isEmpty && isHovered && sourceItem == null && ! isTriggerDropdownOpen && ! isProfileDropdownOpen) setTooltipForNextFrame(font, stack, mouseX, mouseY)
            if (swapRule != null && ! stack.isEmpty) Render2D.drawCenteredString(this, "${swapRule.hotbarSlot + 1}", x + 9, y + 7)
        }

        for (i in 0 until 9) {
            val x = cx - 82 + (i * 18)
            val y = cy + 45
            val stack = inv.getItem(i)
            val rule = config.activeRules[getCurrentTrigger()]?.find { it.hotbarSlot == i }

            val isHovered = mouseX >= x && mouseX <= x + 18 && mouseY >= y && mouseY <= y + 18

            if (! stack.isEmpty && isHovered && sourceItem == null && ! isTriggerDropdownOpen && ! isProfileDropdownOpen) setTooltipForNextFrame(font, stack, mouseX, mouseY)
            if (rule != null) Render2D.drawCenteredString(this, "${i + 1}", x + 9, y + 6)
        }

        sourceItem?.let {
            ItemRenderer.drawBatchedItemStack(this, it, mouseX - 8, mouseY - 8)
            ItemRenderer.endItemRendererBatch(this)
        }
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        val centerX = width / 2
        val centerY = height / 2

        if (isProfileDropdownOpen && config.profiles.isNotEmpty() && (event.button() == 0 || event.button() == 1)) {
            val x = centerX - 75
            val startY = centerY - 78
            val itemHeight = 16
            val profilesList = config.profiles.keys.toList()
            val listHeight = profilesList.size * itemHeight

            if (event.x >= x && event.x <= x + 110 && event.y >= startY && event.y <= startY + listHeight) {
                val clickedIndex = ((event.y - startY) / itemHeight).toInt()
                if (clickedIndex in profilesList.indices) {
                    val clickedProfile = profilesList[clickedIndex]
                    config.activeProfile = clickedProfile
                    selectedTriggerIndex = 0
                    profileSelectorBtn.message = Component.literal(clickedProfile)
                    selectorBtn.message = Component.literal(getCurrentTriggerLabel())
                    Style.playClickSound(1.2f)
                }
                isProfileDropdownOpen = false
                return true
            }

            val btnX = centerX - 75
            val btnY = centerY - 98
            if (event.x >= btnX && event.x <= btnX + 110 && event.y >= btnY && event.y <= btnY + 20) {
                return super.mouseClicked(event, isDoubleClick)
            }
            else isProfileDropdownOpen = false
        }

        if (isTriggerDropdownOpen && triggers.isNotEmpty() && (event.button() == 0 || event.button() == 1)) {
            val x = centerX - 75
            val startY = centerY - 55
            val itemHeight = 16
            val listHeight = triggers.size * itemHeight

            if (event.x >= x && event.x <= x + 110 && event.y >= startY && event.y <= startY + listHeight) {
                val clickedIndex = ((event.y - startY) / itemHeight).toInt()
                if (clickedIndex in triggers.indices) {
                    selectedTriggerIndex = clickedIndex
                    selectorBtn.message = Component.literal(getCurrentTriggerLabel())
                    Style.playClickSound(1.2f)
                    if (event.button() == 1) {
                        openEditTrigger(getCurrentTrigger())
                    }
                }
                isTriggerDropdownOpen = false
                return true
            }

            val btnX = centerX - 75
            val btnY = centerY - 75
            if (event.x >= btnX && event.x <= btnX + 110 && event.y >= btnY && event.y <= btnY + 20) {
                return super.mouseClicked(event, isDoubleClick)
            }
            else isTriggerDropdownOpen = false
        }

        if (! isTriggerDropdownOpen && ! isProfileDropdownOpen && event.button() == 1 && triggers.isNotEmpty()) {
            val btnX = centerX - 75
            val btnY = centerY - 75
            if (event.x >= btnX && event.x <= btnX + 110 && event.y >= btnY && event.y <= btnY + 20) {
                openEditTrigger(getCurrentTrigger())
                return true
            }
        }

        if (isCreatingProfile || isCreatingNew) return super.mouseClicked(event, isDoubleClick)
        if (event.button() != 0 || triggers.isEmpty()) return super.mouseClicked(event, isDoubleClick)

        val player = minecraft?.player ?: return false
        val inv = player.inventory

        for (i in 0 until 27) {
            val x = centerX - 82 + ((i % 9) * 18)
            val y = centerY - 20 + ((i / 9) * 18)
            if (event.x >= x && event.x <= x + 18 && event.y >= y && event.y <= y + 18) {
                val stack = inv.getItem(i + 9)
                if (! stack.isEmpty) {
                    sourceItem = stack.copy()
                    sourceSlot = i + 9
                    Style.playClickSound(1.1f)
                }
                return true
            }
        }

        for (i in 0 until 9) {
            val x = centerX - 82 + (i * 18)
            val y = centerY + 45
            if (event.x >= x && event.x <= x + 18 && event.y >= y && event.y <= y + 18) {
                val trigger = getCurrentTrigger()
                val rules = config.activeRules.getOrPut(trigger) { mutableListOf() }
                val stack = sourceItem
                if (stack != null) {
                    val id = stack.itemUUID.ifBlank { stack.skyblockId }.ifBlank { stack.hoverName.string }
                    rules.removeIf { it.hotbarSlot == i || it.id == id }
                    rules.add(SwapRule(id, i))
                    sourceItem = null
                    Style.playClickSound(1.5f)
                }
                else {
                    rules.removeIf { it.hotbarSlot == i }
                    Style.playClickSound(0.8f)
                }
                return true
            }
        }

        sourceItem = null
        return super.mouseClicked(event, isDoubleClick)
    }

    private fun refreshVisibility() {
        val editingTriggerActive = isCreatingNew
        nameInput.visible = editingTriggerActive
        messageInput.visible = editingTriggerActive
        saveTriggerBtn.visible = editingTriggerActive
        cancelTriggerBtn.visible = editingTriggerActive
        enabledToggleBtn.visible = editingTriggerActive

        val editingProfileActive = isCreatingProfile
        profileNameInput.visible = editingProfileActive
        saveProfileBtn.visible = editingProfileActive
        cancelProfileBtn.visible = editingProfileActive

        val normalState = ! isCreatingNew && ! isCreatingProfile
        selectorBtn.visible = normalState
        createButton.visible = normalState
        deleteBtn.visible = normalState
        profileSelectorBtn.visible = normalState
        createProfileBtn.visible = normalState
        deleteProfileBtn.visible = normalState
    }

    private fun getCurrentTrigger() = triggers.getOrNull(selectedTriggerIndex) ?: "None"
    private fun getCurrentTriggerLabel() = triggers.getOrNull(selectedTriggerIndex)?.let { if (it !in config.activeDisabled) it else "$it (off)" } ?: "None"
    private fun updateEnabledButton() {
        enabledToggleBtn.message = Component.literal(if (editEnabled) "Enabled" else "Disabled")
    }

    private fun openEditTrigger(trigger: String) {
        isCreatingNew = true
        isTriggerDropdownOpen = false
        editingTrigger = trigger
        nameInput.value = trigger
        messageInput.value = config.activeTriggers[trigger] ?: ""
        editEnabled = trigger !in config.activeDisabled
        sourceItem = null
        sourceSlot = - 1
        updateEnabledButton()
        refreshVisibility()
    }

    private fun closeEditor() {
        isCreatingNew = false
        isTriggerDropdownOpen = false
        editingTrigger = null
        sourceItem = null
        sourceSlot = - 1
        nameInput.value = ""
        messageInput.value = ""
        refreshVisibility()
    }

    private fun closeProfileEditor() {
        isCreatingProfile = false
        isProfileDropdownOpen = false
        profileNameInput.value = ""
        refreshVisibility()
    }
}
//#endif