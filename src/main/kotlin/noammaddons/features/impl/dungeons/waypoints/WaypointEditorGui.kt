package noammaddons.features.impl.dungeons.waypoints

import gg.essential.elementa.utils.withAlpha
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.util.BlockPos
import noammaddons.utils.ChatUtils
import java.awt.Color

class WaypointEditorGui(
    private val roomName: String,
    private val absolutePos: BlockPos,
    private val relativePos: BlockPos,
    private val initialState: DungeonWaypoints.DungeonWaypoint? = null
): GuiScreen() {
    private var filled = true
    private var outline = true
    private var phase = true
    private var colorIndex = 0

    private val colors = listOf(
        Color.GREEN, Color.RED, Color.BLUE, Color.CYAN,
        Color.MAGENTA, Color.YELLOW, Color.WHITE, Color.BLACK, Color.ORANGE
    )
    private val colorNames = listOf(
        "Green", "Red", "Blue", "Cyan",
        "Magenta", "Yellow", "White", "Black", "Orange"
    )

    override fun initGui() {
        super.initGui()

        if (initialState != null) {
            filled = initialState.filled
            outline = initialState.outline
            phase = initialState.phase

            val initialRgb = initialState.color.rgb and 0xFFFFFF
            colorIndex = colors.indexOfFirst { (it.rgb and 0xFFFFFF) == initialRgb }
            if (colorIndex == - 1) colorIndex = 0
        }

        val centerX = this.width / 2
        val centerY = this.height / 2

        buttonList.add(GuiButton(0, centerX - 100, centerY - 60, "Filled: $filled"))
        buttonList.add(GuiButton(1, centerX - 100, centerY - 35, "Outline: $outline"))
        buttonList.add(GuiButton(2, centerX - 100, centerY - 10, "Phase (See-Thru): $phase"))
        buttonList.add(GuiButton(3, centerX - 100, centerY + 15, "Color: ${colorNames[colorIndex]}"))

        buttonList.add(GuiButton(4, centerX - 100, centerY + 50, 98, 20, "§aSave"))
        buttonList.add(GuiButton(5, centerX + 2, centerY + 50, 98, 20, "§cCancel"))
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            0 -> {
                filled = ! filled
                button.displayString = "Filled: $filled"
            }

            1 -> {
                outline = ! outline
                button.displayString = "Outline: $outline"
            }

            2 -> {
                phase = ! phase
                button.displayString = "Phase (See-Thru): $phase"
            }

            3 -> {
                colorIndex ++
                if (colorIndex >= colors.size) colorIndex = 0
                button.displayString = "Color: ${colorNames[colorIndex]}"
            }

            4 -> {
                if (! filled && ! outline) return ChatUtils.modMessage("Both Filled and Outline cannot be false")
                val selectedColor = colors[colorIndex].let { if (filled) it.withAlpha(60) else it }

                DungeonWaypoints.saveWaypoint(
                    absolutePos, relativePos,
                    roomName, selectedColor,
                    filled, outline, phase
                )

                mc.thePlayer.closeScreen()
            }

            5 -> mc.thePlayer.closeScreen()
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        this.drawDefaultBackground()
        this.drawCenteredString(fontRendererObj, "§b$roomName", this.width / 2, this.height / 2 - 100, 0xFFFFFF)
        this.drawCenteredString(fontRendererObj, "§b${if (initialState == null) "Add" else "Edit"} Dungeon Waypoint", this.width / 2, this.height / 2 - 90, 0xFFFFFF)
        this.drawCenteredString(fontRendererObj, "§7At: ${absolutePos.x}, ${absolutePos.y}, ${absolutePos.z}", this.width / 2, this.height / 2 - 80, 0xAAAAAA)
        super.drawScreen(mouseX, mouseY, partialTicks)
    }
}