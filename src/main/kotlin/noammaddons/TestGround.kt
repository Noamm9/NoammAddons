package noammaddons

import kotlinx.coroutines.*
import net.minecraft.client.gui.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraft.network.play.server.S45PacketTitle
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.player.AttackEntityEvent
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.*
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import noammaddons.events.*
import noammaddons.features.FeatureManager.registerFeatures
import noammaddons.features.impl.DevOptions
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonScanner
import noammaddons.features.impl.dungeons.solvers.devices.AutoI4.testi4
import noammaddons.features.impl.esp.StarMobESP
import noammaddons.noammaddons.Companion.ahData
import noammaddons.noammaddons.Companion.mc
import noammaddons.noammaddons.Companion.scope
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.DungeonUtils.dungeonEnded
import noammaddons.utils.DungeonUtils.dungeonStarted
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.LocationUtils.P3Section
import noammaddons.utils.LocationUtils.dungeonFloor
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.LocationUtils.onHypixel
import noammaddons.utils.LocationUtils.world
import noammaddons.utils.MathUtils.destructured
import noammaddons.utils.RenderHelper.getScaleFactor
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.RenderUtils2D.modelViewMatrix
import noammaddons.utils.RenderUtils2D.projectionMatrix
import noammaddons.utils.RenderUtils2D.viewportDims
import noammaddons.utils.ScanUtils.currentRoom
import noammaddons.utils.ScanUtils.getCore
import noammaddons.utils.ScanUtils.getRoomCenterAt
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.send
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3


object TestGround {
    private val titles = mutableListOf<String>()
    private var fuckingBitch = false
    private var sent = false
    private var a = false

    @SubscribeEvent
    fun adfd(event: ItemTooltipEvent) {
        if (! DevOptions.devMode) return
        val sbid = event.itemStack.SkyblockID ?: return
        event.toolTip?.add("SkyblockID: &6$sbid".addColor())
    }

    @SubscribeEvent
    @Suppress("Unused_parameter")
    fun t(event: RenderOverlay) {
        if (! DevOptions.isDev) return
        GlStateManager.pushMatrix()
        val scale = 2f / mc.getScaleFactor()
        GlStateManager.scale(scale, scale, scale)

        drawText(
            listOf(
                "dungeonStarted: $dungeonStarted",
                "dungeonEnded: $dungeonEnded",
                "indungeons: $inDungeon",
                "dungeonfloor: $dungeonFloor",
                "dungeonfloorNumber: $dungeonFloorNumber",
                "inboss: $inBoss",
                "inSkyblock: $inSkyblock",
                "onHypixel: $onHypixel",
                "F7Phase: $F7Phase",
                "P3Section: $P3Section",
                "WorldName: $world"
            ).joinToString("\n"),
            200f, 10f
        )

        val rc = getRoomCenterAt(mc.thePlayer.position)

        drawText(
            """
			getCore: ${getCore(rc.x, rc.z)}
			currentRoom: ${currentRoom?.data?.name ?: "&cUnknown&r"}
			getRoomCenter: $rc
		""".trimIndent(),
            150f, 100f, 1f,
            Color.CYAN
        )


        drawText(
            """Party Leader: ${PartyUtils.leader}
				| Party Size: ${PartyUtils.size}
				| Party Members: ${"\n" + PartyUtils.members.entries.joinToString("\n")}
			""".trimMargin(),
            20f, 200f, 1f,
            Color.PINK
        )

        GlStateManager.popMatrix()
    }

    @SubscribeEvent
    fun onMessage(event: MessageSentEvent) {
        if (! DevOptions.isDev) return
        modMessage(event.message)

        when (event.message) {
            "i4" -> {
                event.isCanceled = true
                scope.launch {
                    testi4()
                }
            }

            "chat" -> {
                event.isCanceled = true
                setTimeout(1000) { sendChatMessage("/play sb") }
            }

            "e" -> {
                event.isCanceled = true
                modMessage(
                    mc.theWorld.loadedEntityList
                        .filterIsInstance<EntityArmorStand>()
                        .filter { it.getDistanceToEntity(mc.thePlayer) < 5 }
                )
            }

            "leap" -> {
                event.isCanceled = true
                ActionUtils.leap("WebbierAmoeba0")
            }

            "ah" -> {
                event.isCanceled = true
                modMessage(ahData["TERMINATOR"]?.toInt())
            }

            "test" -> {
                event.isCanceled = true
                scope.launch {
                    registerFeatures()
                }
                SoundUtils.chipiChapa()
            }

            "esp" -> {
                event.isCanceled = true
                StarMobESP.starMobs.clear()
                StarMobESP.checked.clear()
            }

            "scan" -> {
                event.isCanceled = true
                DungeonScanner.scan()
                SoundUtils.Pling()
            }

            "nbt" -> {
                event.isCanceled = true
                val held = mc.thePlayer?.heldItem ?: return
                val nbt = held.getSubCompound("ExtraAttributes", false) ?: return
                modMessage(nbt)
            }

            "fullbright" -> {
                event.isCanceled = true
                mc.gameSettings.gammaSetting = 100000f
            }

            "swap" -> {
                event.isCanceled = true
                ActionUtils.quickSwapTo(ServerPlayer.player.getHeldItem()?.SkyblockID ?: return)
            }

            "secrets" -> {
                event.isCanceled = true
                CoroutineScope(Dispatchers.IO).launch {
                    DungeonUtils.dungeonTeammates.toList().forEach { player ->
                        val s = ProfileUtils.getSecrets(player.name)
                        modMessage("${player.name} has $s secrets")
                    }
                }
            }

            "cri" -> {
                event.isCanceled = true
                GuiUtils.openScreen(SimpleVanillaConfigGui(mc.currentScreen))
            }

            "t" -> titles.forEach(::modMessage)

        }
    }

    @SubscribeEvent
    fun handlePartyCommands(event: MessageSentEvent) {
        if (! onHypixel) return
        val msg = event.message.removeFormatting().lowercase()

        if (msg == "/p invite accept") {
            event.isCanceled = true
            a = true
            sent = false
            setTimeout(250) { a = false }
            return
        }

        if (a && ! sent) {
            if (msg.startsWith("/p invite ") || msg.startsWith("/party accept ")) {
                event.isCanceled = true

                val modifiedMessage = msg
                    .replace("/party accept ", "/p join ")
                    .replace("/p invite ", "/p join ")

                sendChatMessage(modifiedMessage)
                sent = true
            }
        }

        if (msg.equalsOneOf("/pll", "/pl")) {
            event.isCanceled = true
            C01PacketChatMessage("/pl").send()
        }
    }

    @SubscribeEvent
    fun wtf(event: WorldLoadPostEvent) {
        setTimeout(500) {
            if (mc.currentScreen !is GuiDownloadTerrain) return@setTimeout
            mc.currentScreen = null
        }
    }

    @SubscribeEvent
    fun fucklocraw(event: FMLNetworkEvent.ClientConnectedToServerEvent) {
        if (event.isLocal) return
        fuckingBitch = true
        setTimeout(5000) { fuckingBitch = false }
    }

    @SubscribeEvent
    fun fuckLocraw2(event: PacketEvent.Sent) {
        if (! fuckingBitch) return
        val packet = event.packet as? C01PacketChatMessage ?: return
        if (packet.message != "/locraw") return
        debugMessage("Cancelling /locraw")
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onPlaterInteract(e: AttackEntityEvent) {
        if (! inDungeon) return
        if (e.entityPlayer != mc.thePlayer) return
        if (DungeonUtils.dungeonTeammates.none { it.entity == e.target }) return
        e.isCanceled = true
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun afad(event: Event) {
        when (event) {
            is WorldUnloadEvent -> ScoreboardUtils.sidebarLines = emptyList()
            is PacketEvent.Received -> with(event.packet) {
                if (! DevOptions.trackTitles) return
                if (this !is S45PacketTitle) return@with
                titles.add("type: $type, msg: ${message?.formattedText}")
            }

            is ClickEvent.RightClickEvent -> {
                if (! DevOptions.printBlockCoords) return
                if (! mc.thePlayer.isSneaking) return
                val pos = mc.objectMouseOver?.blockPos ?: return

                modMessage("{\"x\": ${pos.x}, \"y\": ${pos.y}, \"z\": ${pos.z}}")
                BlockUtils.toAir(pos)
            }

            else -> {}
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRenderWorld(event: RenderWorldLastEvent) {
        val (x, y, z) = mc.thePlayer?.renderVec?.destructured() ?: return
        GlStateManager.pushMatrix()
        GlStateManager.translate(- x, - y, - z)

        glGetFloat(GL_MODELVIEW_MATRIX, modelViewMatrix)
        glGetFloat(GL_PROJECTION_MATRIX, projectionMatrix)

        GlStateManager.popMatrix()
        glGetInteger(GL_VIEWPORT, viewportDims)
    }
}


// Define different types of settings
sealed class SettingType {
    object BOOLEAN: SettingType()
    data class INTEGER(val min: Int, val max: Int): SettingType()
    object STRING: SettingType()
    // Add other types like DOUBLE, ENUM, COLOR etc. as needed
}

// Generic Setting data class
data class Setting<T>(
    val id: String, // Unique identifier
    val name: String,
    var value: T,
    val type: SettingType,
    val description: String? = null
)

// Placeholder object to hold and manage settings
object ModConfig {
    // Group settings by category
    val settingsByCategory = mutableMapOf<String, MutableList<Setting<*>>>()

    init {
        // --- Visuals Category ---
        val visuals = mutableListOf<Setting<*>>(
            Setting("showHud", "Show HUD", true, SettingType.BOOLEAN, "Toggles the main HUD display."),
            Setting("hudScale", "HUD Scale", 10, SettingType.INTEGER(5, 20), "Sets the scale of the HUD elements (5-20)."), // Representing as 0.5 to 2.0 internally maybe
            Setting("hudTheme", "HUD Theme", "Default", SettingType.STRING, "Enter the name of the HUD theme.")
        )
        settingsByCategory["Visuals"] = visuals

        // --- Combat Category ---
        val combat = mutableListOf<Setting<*>>(
            Setting("enableReach", "Enable Reach", false, SettingType.BOOLEAN, "Extends attack reach (use with caution)."),
            Setting("reachDistance", "Reach Distance", 35, SettingType.INTEGER(30, 60), "Reach distance (3.0 to 6.0 blocks).") // Example: value is distance * 10
        )
        settingsByCategory["Combat"] = combat

        // --- Misc Category ---
        val misc = mutableListOf<Setting<*>>(
            Setting("autoSprint", "Auto Sprint", true, SettingType.BOOLEAN, "Automatically sprints when moving forward.")
        )
        settingsByCategory["Misc"] = misc

        // Load config from file here (e.g., using Gson, kotlinx.serialization)
        load()
    }

    fun getCategories(): List<String> = settingsByCategory.keys.sorted()

    fun getSettings(category: String): List<Setting<*>> = settingsByCategory[category] ?: emptyList()

    // Placeholder save/load functions
    fun save() {
        println("Saving configuration...")
        // Add actual saving logic here (e.g., write to a JSON file)
        settingsByCategory.forEach { (category, settings) ->
            println(" [$category]")
            settings.forEach { setting ->
                println("  ${setting.name}: ${setting.value}")
            }
        }
        println("Configuration saved.")
    }

    fun load() {
        println("Loading configuration...")
        // Add actual loading logic here (e.g., read from a JSON file)
        println("Configuration loaded (placeholder).")
    }

    // Helper to get a setting's value (example)
    inline fun <reified T> getValue(categoryId: String, settingId: String, default: T): T {
        val setting = settingsByCategory[categoryId]?.find { it.id == settingId }
        return setting?.value as? T ?: default
    }
}

// Helper function for color conversion (you might already have this)
fun Color.toInt(): Int {
    return this.rgb
}

// Basic GuiScreen implementation
class SimpleVanillaConfigGui(private val previousScreen: GuiScreen? = null): GuiScreen() {

    // --- State Variables ---
    private var selectedCategory: String = ModConfig.getCategories().firstOrNull() ?: "None"
    private var selectedFeatureId: String? = null // ID of the selected feature/module

    private var categoryScroll: Float = 0f
    private var featureScroll: Float = 0f
    private var settingsScroll: Float = 0f

    private var maxCategoryScroll: Float = 0f
    private var maxFeatureScroll: Float = 0f
    private var maxSettingsScroll: Float = 0f

    // Temporary list for filtered features (based on category)
    private var currentFeatures: List<Setting<*>> = emptyList() // Assuming features are also represented as Settings for simplicity here

    // Temporary list for settings of the selected feature
    private var currentSettings: List<Setting<*>> = emptyList() // This would normally be settings *within* a feature

    // --- Layout Variables (Calculated in initGui) ---
    private var guiX: Int = 0
    private var guiY: Int = 0
    private var guiWidth: Int = 0
    private var guiHeight: Int = 0

    private var categoryPanelWidth: Int = 0
    private var featurePanelWidth: Int = 0
    private var settingsPanelWidth: Int = 0

    private var panelPadding: Int = 4
    private var itemHeight: Int = 15
    private var categoryHeaderHeight = 12
    private var categoryItemHeight = 13
    private var featureItemHeight = 25 // Taller for toggle/button
    private var settingItemHeight = 18

    // Colors
    private val colorBg = Color(15, 15, 15).toInt()
    private val colorPanel = Color(30, 30, 30).toInt()
    private val colorSelected = Color(0, 170, 170).toInt()
    private val colorHover = Color(60, 60, 60).toInt()
    private val colorText = Color.WHITE.toInt()
    private val colorHeader = Color.LIGHT_GRAY.toInt()
    private val colorToggleEnabled = Color(0, 170, 170).toInt()
    private val colorToggleDisabled = Color(80, 80, 80).toInt()
    private val colorKnob = Color.WHITE.toInt()


    override fun initGui() {
        super.initGui()
        val sr = ScaledResolution(mc)
        val screenW = sr.scaledWidth
        val screenH = sr.scaledHeight

        // Define GUI size relative to screen
        guiWidth = (screenW * 0.8).toInt()
        guiHeight = (screenH * 0.8).toInt()
        guiX = (screenW - guiWidth) / 2
        guiY = (screenH - guiHeight) / 2

        // Define panel widths (example split)
        categoryPanelWidth = (guiWidth * 0.20).toInt()
        featurePanelWidth = (guiWidth * 0.40).toInt()
        settingsPanelWidth = guiWidth - categoryPanelWidth - featurePanelWidth // Remaining space

        // Initial update of feature list based on selected category
        updateCurrentFeatures()
        updateCurrentSettings() // Initially empty or based on first feature
    }

    // Update the list of features to display based on the selected category
    private fun updateCurrentFeatures() {
        // In a real scenario, features/modules would be separate from settings
        // Here, we simplify: assume ModConfig holds "features" grouped by category
        // You'd likely have a map like `Map<String, List<Feature>>`
        currentFeatures = ModConfig.getSettings(selectedCategory) // Placeholder: treat settings as features for now
        selectedFeatureId = currentFeatures.firstOrNull()?.id // Select the first feature automatically
        featureScroll = 0f // Reset scroll
        updateCurrentSettings() // Update settings panel based on new selection
    }

    // Update the list of settings for the currently selected feature
    private fun updateCurrentSettings() {
        // In a real scenario, each Feature object would have its own list of Settings
        // Here, we simplify: assume the selected feature *is* a setting, and it has no sub-settings
        // Replace this with actual logic to get settings for the selectedFeatureId
        val selectedFeature = currentFeatures.find { it.id == selectedFeatureId }
        currentSettings = if (selectedFeature != null) {
            // TODO: Replace this placeholder: Get actual settings *for* selectedFeature
            listOf(
                Setting("desc", "Description", "This is a placeholder description for ${selectedFeature.name}.", SettingType.STRING),
                Setting("key", "Keybind", "[ NONE ]", SettingType.STRING), // Placeholder for keybind setting
                Setting("bool_ex", "Example Toggle", true, SettingType.BOOLEAN)
            )
        }
        else {
            emptyList()
        }
        settingsScroll = 0f // Reset scroll
    }


    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        // Draw main background (semi-transparent overlay)
        drawRect(0, 0, width, height, Color(0, 0, 0, 100).toInt()) // Use GuiScreen width/height

        // Draw GUI background
        drawRect(guiX, guiY, guiX + guiWidth, guiY + guiHeight, colorBg)

        // Calculate panel bounds
        val categoryPanelX = guiX
        val featurePanelX = categoryPanelX + categoryPanelWidth
        val settingsPanelX = featurePanelX + featurePanelWidth

        // --- Draw Panels ---
        drawRect(categoryPanelX, guiY, featurePanelX, guiY + guiHeight, colorPanel)
        drawRect(featurePanelX, guiY, settingsPanelX, guiY + guiHeight, colorPanel)
        drawRect(settingsPanelX, guiY, guiX + guiWidth, guiY + guiHeight, colorPanel)

        // --- Draw Content (with basic clipping simulation) ---
        val contentY = guiY + panelPadding
        val contentHeight = guiHeight - panelPadding * 2

        maxCategoryScroll = drawCategories(mouseX, mouseY, categoryPanelX + panelPadding, contentY, categoryPanelWidth - panelPadding * 2, contentHeight)
        maxFeatureScroll = drawFeatures(mouseX, mouseY, featurePanelX + panelPadding, contentY, featurePanelWidth - panelPadding * 2, contentHeight)
        maxSettingsScroll = drawSettings(mouseX, mouseY, settingsPanelX + panelPadding, contentY, settingsPanelWidth - panelPadding * 2, contentHeight)

        // --- Draw Scrollbars (very basic representation) ---
        drawScrollBar(categoryPanelX + categoryPanelWidth - 3, contentY, 2, contentHeight, categoryScroll, maxCategoryScroll)
        drawScrollBar(featurePanelX + featurePanelWidth - 3, contentY, 2, contentHeight, featureScroll, maxFeatureScroll)
        drawScrollBar(settingsPanelX + settingsPanelWidth - 3, contentY, 2, contentHeight, settingsScroll, maxSettingsScroll)

        super.drawScreen(mouseX, mouseY, partialTicks) // Allow GuiScreen to draw tooltips, etc.
    }

    // --- Drawing Functions for Each Panel ---

    private fun drawCategories(mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, height: Int): Float {
        var currentY = y - categoryScroll.toInt() // Start drawing from scrolled position
        var totalContentHeight = 0f

        // Group categories (example structure)
        val categoryGroups = ModConfig.settingsByCategory.keys.groupBy {
            // Simple grouping logic (replace with your actual grouping)
            when (it) {
                "Visuals", "Combat", "Movement", "Player" -> "PVP" // Example grouping
                "Skills", "Mining", "Slayer", "Rift", "QOL" -> "Skyblock"
                "Main", "Floor 7", "Puzzle" -> "Dungeons"
                else -> "Misc"
            }
        }.toSortedMap() // Sort group names

        val groupOrder = listOf("Skyblock", "Dungeons", "PVP", "Misc") // Define group order

        for (groupName in groupOrder) {
            val categoriesInGroup = categoryGroups[groupName]?.sorted() ?: continue

            // Draw Group Header
            if (currentY + categoryHeaderHeight > y && currentY < y + height) { // Basic clipping check
                fontRendererObj.drawString(groupName, x, currentY + 2, colorHeader)
            }
            currentY += categoryHeaderHeight
            totalContentHeight += categoryHeaderHeight

            // Draw Categories in Group
            for (categoryName in categoriesInGroup) {
                val itemEndY = currentY + categoryItemHeight
                val isHovered = isMouseOver(mouseX, mouseY, x, currentY, width, categoryItemHeight)
                val isSelected = categoryName == selectedCategory

                // Basic clipping check
                if (itemEndY > y && currentY < y + height) {
                    var bgColor = if (isSelected) colorSelected else 0 // Transparent if not selected
                    if (isHovered && ! isSelected) {
                        bgColor = colorHover
                    }
                    if (bgColor != 0) {
                        drawRect(x, currentY, x + width, itemEndY, bgColor)
                    }
                    // Add indicator for selected item
                    val indicatorText = if (isSelected) "> " else "• "
                    fontRendererObj.drawString(indicatorText + categoryName, x + 4, currentY + 3, colorText)
                }
                // Add interaction bounds even if clipped for scrolling calculation
                totalContentHeight += categoryItemHeight
                currentY += categoryItemHeight
            }
            currentY += 5 // Spacing between groups
            totalContentHeight += 5f
        }
        return (totalContentHeight - height.toFloat()).coerceAtLeast(0f)
        // Max scroll is total height minus visible height
    }


    private fun drawFeatures(mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, height: Int): Float {
        var currentY = y - featureScroll.toInt()
        var totalContentHeight = 0f

        for (feature in currentFeatures) {
            val itemEndY = currentY + featureItemHeight
            val isHovered = isMouseOver(mouseX, mouseY, x, currentY, width, featureItemHeight)
            val isSelected = feature.id == selectedFeatureId

            // Basic clipping check
            if (itemEndY > y && currentY < y + height) {
                var bgColor = if (isSelected) colorSelected else colorPanel // Use panel color as base
                if (isHovered) {
                    bgColor = colorHover
                }
                // Draw background (NO rounded corners in this simple version)
                drawRect(x, currentY, x + width, itemEndY, bgColor)

                // Draw feature name
                fontRendererObj.drawString(feature.name, x + 5, currentY + (featureItemHeight - fontRendererObj.FONT_HEIGHT) / 2, colorText)

                // Draw basic toggle switch visual
                val toggleWidth = 25
                val toggleHeight = 12
                val toggleX = x + width - toggleWidth - 5
                val toggleY = currentY + (featureItemHeight - toggleHeight) / 2
                val featureEnabled = feature.value as? Boolean ?: false // Assuming features are BOOLEAN settings for enable/disable
                drawBasicToggle(toggleX, toggleY, toggleWidth, toggleHeight, featureEnabled)

                // Draw settings button placeholder (three dots)
                fontRendererObj.drawString("...", x + width - toggleWidth - 15, toggleY + 1, colorText)

            }
            totalContentHeight += featureItemHeight + 2 // Add spacing
            currentY += featureItemHeight + 2 // Add spacing
        }
        return (totalContentHeight - height.toFloat()).coerceAtLeast(0f)
    }

    private fun drawSettings(mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, height: Int): Float {
        var currentY = y - settingsScroll.toInt()
        var totalContentHeight = 0f

        if (selectedFeatureId == null) {
            if (currentY + settingItemHeight > y && currentY < y + height) {
                fontRendererObj.drawString("Select a feature", x + 5, currentY + 5, colorHeader)
            }
            totalContentHeight = settingItemHeight.toFloat()
            return (totalContentHeight - height.toFloat()).coerceAtLeast(0f)
        }

        for (setting in currentSettings) {
            val itemEndY = currentY + settingItemHeight

            // Basic clipping check
            if (itemEndY > y && currentY < y + height) {
                // Draw setting name
                fontRendererObj.drawString(setting.name, x + 5, currentY + (settingItemHeight - fontRendererObj.FONT_HEIGHT) / 2, colorText)

                // Draw control based on type
                val controlX = x + (width * 0.4).toInt() // Position controls to the right
                val controlWidth = width - (controlX - x) - 5

                when (setting.type) {
                    SettingType.BOOLEAN -> {
                        val toggleWidth = 25
                        val toggleHeight = 12
                        val toggleX = x + width - toggleWidth - 5 // Align right
                        val toggleY = currentY + (settingItemHeight - toggleHeight) / 2
                        drawBasicToggle(toggleX, toggleY, toggleWidth, toggleHeight, setting.value as Boolean)
                    }

                    SettingType.STRING -> {
                        // Just display string value (no text input in this simple example)
                        val text = setting.value.toString()
                        val trimmedText = fontRendererObj.trimStringToWidth(text, controlWidth)
                        fontRendererObj.drawString(trimmedText, controlX, currentY + (settingItemHeight - fontRendererObj.FONT_HEIGHT) / 2, colorText)
                    }

                    is SettingType.INTEGER -> {
                        // Just display integer value
                        fontRendererObj.drawString(setting.value.toString(), controlX, currentY + (settingItemHeight - fontRendererObj.FONT_HEIGHT) / 2, colorText)
                    }
                }
            }
            totalContentHeight += settingItemHeight + 2 // Add spacing
            currentY += settingItemHeight + 2 // Add spacing
        }

        return (totalContentHeight - height.toFloat()).coerceAtLeast(0f)
    }

    // --- Helper Drawing Functions ---

    private fun drawBasicToggle(x: Int, y: Int, width: Int, height: Int, enabled: Boolean) {
        val bgColor = if (enabled) colorToggleEnabled else colorToggleDisabled
        drawRect(x, y, x + width, y + height, bgColor) // Background

        val knobWidth = height - 2 // Make knob slightly smaller than height
        val knobX = if (enabled) x + width - knobWidth - 1 else x + 1
        val knobY = y + 1
        drawRect(knobX, knobY, knobX + knobWidth, knobY + height - 2, colorKnob) // Knob
    }

    private fun drawScrollBar(x: Int, y: Int, width: Int, height: Int, scroll: Float, maxScroll: Float) {
        if (maxScroll <= 0f) return // No scrollbar needed

        val scrollBarHeight = (height * (height / (maxScroll + height))).toInt().coerceAtLeast(10)
        val scrollBarY = y + (scroll / maxScroll * (height - scrollBarHeight)).toInt()

        drawRect(x, y, x + width, y + height, Color(10, 10, 10).toInt()) // Background track
        drawRect(x, scrollBarY, x + width, scrollBarY + scrollBarHeight, Color(100, 100, 100).toInt()) // Thumb
    }


    // --- Interaction Handling ---

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseButton == 0) { // Left click
            val contentY = guiY + panelPadding
            val contentHeight = guiHeight - panelPadding * 2

            // Check Category Clicks
            var currentCatY = contentY - categoryScroll.toInt()
            var currentCatTotalH = 0f
            val categoryPanelX = guiX + panelPadding
            val categoryWidth = categoryPanelWidth - panelPadding * 2
            val categoryGroups = ModConfig.settingsByCategory.keys.groupBy { /* Grouping logic */ when (it) {
                "Visuals", "Combat", "Movement", "Player" -> "PVP";"Skills", "Mining", "Slayer", "Rift", "QOL" -> "Skyblock";"Main", "Floor 7", "Puzzle" -> "Dungeons";else -> "Misc"
            }
            }.toSortedMap()
            val groupOrder = listOf("Skyblock", "Dungeons", "PVP", "Misc")
            for (groupName in groupOrder) {
                val categoriesInGroup = categoryGroups[groupName]?.sorted() ?: continue
                currentCatY += categoryHeaderHeight
                currentCatTotalH += categoryHeaderHeight
                for (categoryName in categoriesInGroup) {
                    if (isMouseOver(mouseX, mouseY, categoryPanelX, currentCatY, categoryWidth, categoryItemHeight)) {
                        if (mouseY >= contentY && mouseY < contentY + contentHeight) { // Check if click is within visible bounds
                            selectedCategory = categoryName
                            updateCurrentFeatures() // Update middle panel
                            mc.thePlayer.playSound("gui.button.press", 1.0f, 1.0f)
                            return // Click handled
                        }
                    }
                    currentCatY += categoryItemHeight
                    currentCatTotalH += categoryItemHeight
                }
                currentCatY += 5; currentCatTotalH += 5f
            }


            // Check Feature Clicks
            var currentFeatY = contentY - featureScroll.toInt()
            val featurePanelX = guiX + categoryPanelWidth + panelPadding
            val featureWidth = featurePanelWidth - panelPadding * 2
            for (feature in currentFeatures) {
                val itemEndY = currentFeatY + featureItemHeight
                if (mouseY >= contentY && mouseY < contentY + contentHeight && mouseY >= currentFeatY && mouseY < itemEndY) { // Check visible bounds
                    // Check click on main feature area
                    if (isMouseOver(mouseX, mouseY, featurePanelX, currentFeatY, featureWidth, featureItemHeight)) {
                        // Check if toggle was clicked
                        val toggleWidth = 25;
                        val toggleHeight = 12
                        val toggleX = featurePanelX + featureWidth - toggleWidth - 5
                        val toggleY = currentFeatY + (featureItemHeight - toggleHeight) / 2
                        if (isMouseOver(mouseX, mouseY, toggleX, toggleY, toggleWidth, toggleHeight)) {
                            //  feature.value = ! feature.value
                            ModConfig.save() // Save change
                            mc.thePlayer.playSound("gui.button.press", 1.0f, 0.9f)
                        }
                        else {
                            // Clicked feature row itself (not toggle)
                            selectedFeatureId = feature.id
                            updateCurrentSettings() // Update right panel
                            mc.thePlayer.playSound("gui.button.press", 1.0f, 1.0f)
                        }
                        return // Click handled
                    }
                }
                currentFeatY += featureItemHeight + 2
            }

            // Check Settings Clicks (Example: Toggle)
            var currentSetY = contentY - settingsScroll.toInt()
            val settingsPanelX = guiX + categoryPanelWidth + featurePanelWidth + panelPadding
            val settingsWidth = settingsPanelWidth - panelPadding * 2
            for (setting in currentSettings) {
                val itemEndY = currentSetY + settingItemHeight
                if (setting.type == SettingType.BOOLEAN && mouseY >= contentY && mouseY < contentY + contentHeight && mouseY >= currentSetY && mouseY < itemEndY) {
                    val toggleWidth = 25;
                    val toggleHeight = 12
                    val toggleX = settingsPanelX + settingsWidth - toggleWidth - 5 // Align right
                    val toggleY = currentSetY + (settingItemHeight - toggleHeight) / 2
                    if (isMouseOver(mouseX, mouseY, toggleX, toggleY, toggleWidth, toggleHeight)) {
                        val currentVal = setting.value as? Boolean ?: false
                        //      setting.value = ! currentVal // Toggle
                        ModConfig.save()
                        mc.thePlayer.playSound("gui.button.press", 1.0f, if (! currentVal) 1.1f else 0.9f)
                        return // Click handled
                    }
                }
                currentSetY += settingItemHeight + 2
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        var scrollAmount = Mouse.getEventDWheel()
        if (scrollAmount != 0) {
            scrollAmount = if (scrollAmount > 0) - 1 else 1 // Normalize scroll direction

            val scrollSpeed = 15 // Pixels per scroll wheel click

            val mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth
            val mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1

            // Determine which panel to scroll based on mouse position
            val featurePanelX = guiX + categoryPanelWidth
            val settingsPanelX = featurePanelX + featurePanelWidth

            when {
                mouseX >= guiX && mouseX < featurePanelX -> { // Category Panel
                    categoryScroll = (categoryScroll + scrollAmount * scrollSpeed).coerceIn(0f, maxCategoryScroll)
                }

                mouseX >= featurePanelX && mouseX < settingsPanelX -> { // Feature Panel
                    featureScroll = (featureScroll + scrollAmount * scrollSpeed).coerceIn(0f, maxFeatureScroll)
                }

                mouseX >= settingsPanelX && mouseX < guiX + guiWidth -> { // Settings Panel
                    settingsScroll = (settingsScroll + scrollAmount * scrollSpeed).coerceIn(0f, maxSettingsScroll)
                }
            }
        }
    }


    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == 1) { // Escape key
            this.mc.displayGuiScreen(previousScreen)
        }
        // Add handling for search bar, keybinds etc. here
    }

    // Required override
    override fun doesGuiPauseGame(): Boolean {
        return false // Config GUIs usually don't pause the game
    }

    // --- Helper Methods ---
    private fun isMouseOver(mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, height: Int): Boolean {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
    }
}