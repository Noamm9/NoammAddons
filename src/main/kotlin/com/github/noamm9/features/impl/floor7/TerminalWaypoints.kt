package com.github.noamm9.features.impl.floor7

import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.CategorySetting
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SeparatorSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.section
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import java.awt.Color

object TerminalWaypoints : Feature("Renders waypoints for P3 F7 terminals and levers with class assignments.") {

    private sealed class Node(val mainPos: BlockPos, val textPos: BlockPos, val defaultClass: DungeonClass, val section: Int) {
        class Terminal(mainPos: BlockPos, textPos: BlockPos, defaultClass: DungeonClass, section: Int) : Node(mainPos, textPos, defaultClass, section)
        class Lever(mainPos: BlockPos, textPos: BlockPos, defaultClass: DungeonClass, section: Int) : Node(mainPos, textPos, defaultClass, section)
    }

    private val allNodes = listOf(
        // Section 1
        Node.Terminal(BlockPos(111, 113, 73), BlockPos(110, 113, 73), DungeonClass.Tank, 1),
        Node.Terminal(BlockPos(111, 119, 79), BlockPos(110, 119, 79), DungeonClass.Tank, 1),
        Node.Terminal(BlockPos(89, 112, 92), BlockPos(90, 112, 92), DungeonClass.Mage, 1),
        Node.Terminal(BlockPos(89, 122, 101), BlockPos(90, 122, 101), DungeonClass.Mage, 1),
        Node.Lever(BlockPos(94, 124, 113), BlockPos(94, 125, 113), DungeonClass.Archer, 1),
        Node.Lever(BlockPos(106, 124, 113), BlockPos(106, 125, 113), DungeonClass.Archer, 1),
        // Section 2
        Node.Terminal(BlockPos(68, 109, 121), BlockPos(68, 109, 122), DungeonClass.Tank, 2),
        Node.Terminal(BlockPos(59, 120, 122), BlockPos(59, 119, 123), DungeonClass.Mage, 2),
        Node.Terminal(BlockPos(47, 109, 121), BlockPos(47, 109, 122), DungeonClass.Berserk, 2),
        Node.Terminal(BlockPos(39, 108, 143), BlockPos(39, 108, 142), DungeonClass.Archer, 2),
        Node.Terminal(BlockPos(40, 124, 122), BlockPos(40, 124, 123), DungeonClass.Berserk, 2),
        Node.Lever(BlockPos(27, 124, 127), BlockPos(27, 125, 127), DungeonClass.Archer, 2),
        Node.Lever(BlockPos(23, 132, 138), BlockPos(23, 133, 138), DungeonClass.Healer, 2),
        // Section 3
        Node.Terminal(BlockPos(-3, 109, 112), BlockPos(-2, 109, 112), DungeonClass.Tank, 3),
        Node.Terminal(BlockPos(-3, 119, 93), BlockPos(-2, 119, 93), DungeonClass.Healer, 3),
        Node.Terminal(BlockPos(19, 123, 93), BlockPos(18, 123, 93), DungeonClass.Berserk, 3),
        Node.Terminal(BlockPos(-3, 109, 77), BlockPos(-2, 109, 77), DungeonClass.Archer, 3),
        Node.Lever(BlockPos(14, 122, 55), BlockPos(14, 123, 55), DungeonClass.Archer, 3),
        Node.Lever(BlockPos(2, 122, 55), BlockPos(2, 123, 55), DungeonClass.Archer, 3),
        // Section 4
        Node.Terminal(BlockPos(41, 109, 29), BlockPos(41, 109, 30), DungeonClass.Tank, 4),
        Node.Terminal(BlockPos(44, 121, 29), BlockPos(44, 121, 30), DungeonClass.Archer, 4),
        Node.Terminal(BlockPos(67, 109, 29), BlockPos(67, 109, 30), DungeonClass.Berserk, 4),
        Node.Terminal(BlockPos(72, 115, 48), BlockPos(72, 114, 47), DungeonClass.Healer, 4),
        Node.Lever(BlockPos(86, 128, 46), BlockPos(86, 129, 46), DungeonClass.Healer, 4),
        Node.Lever(BlockPos(84, 121, 34), BlockPos(84, 122, 34), DungeonClass.Healer, 4),
    )

    private val classOptions = listOf("Healer", "Mage", "Berserk", "Archer", "Tank")
    private val classByIndex = listOf(DungeonClass.Healer, DungeonClass.Mage, DungeonClass.Berserk, DungeonClass.Archer, DungeonClass.Tank)

    private val checkClass by ToggleSetting("Check Class").withDescription("Only render terminals assigned to your dungeon class").section("Settings")
    private val showText by ToggleSetting("Show Text", true).withDescription("Render class label above each terminal")
    private val depthTest by ToggleSetting("See Through Walls").withDescription("Render waypoints through walls")
    private val highlightStyle by DropdownSetting("Highlight Style", 2, listOf("Outline", "Fill", "Both"))
    private val terminalColor by ColorSetting("Terminal Color", Color(0, 255, 255, 200))
    private val leverColor by ColorSetting("Lever Color", Color(255, 255, 0, 200))

    private val nodeClassSettings = mutableMapOf<Node, DropdownSetting>()

    override fun init() {
        val nodesBySection = allNodes.groupBy { it.section }
        for (section in 1..4) {
            val sectionNodes = nodesBySection[section] ?: continue
            val terminals = sectionNodes.filterIsInstance<Node.Terminal>()
            val levers = sectionNodes.filterIsInstance<Node.Lever>()

            configSettings.add(SeparatorSetting())
            configSettings.add(CategorySetting("Section $section"))

            for (node in sectionNodes) {
                val label = when (node) {
                    is Node.Terminal -> "S$section Terminal ${terminals.indexOf(node) + 1}"
                    is Node.Lever -> "S$section ${if (levers.indexOf(node) == 0) "Right" else "Left"} Lever"
                }
                val defaultIdx = classByIndex.indexOf(node.defaultClass).coerceAtLeast(0)
                val setting = DropdownSetting(label, defaultIdx, classOptions)
                configSettings.add(setting)
                nodeClassSettings[node] = setting
            }
        }

        register<RenderWorldEvent> {
            val section = LocationUtils.P3Section ?: return@register
            val playerClass = DungeonListener.thePlayer?.clazz
            val outline = highlightStyle.value != 1
            val fill = highlightStyle.value != 0
            val phase = depthTest.value

            for (node in allNodes) {
                if (node.section != section) continue

                val assignedClass = classByIndex.getOrNull(nodeClassSettings[node]?.value ?: -1) ?: node.defaultClass
                if (checkClass.value && playerClass != null && playerClass != assignedClass) continue

                val color = if (node is Node.Lever) leverColor.value else terminalColor.value
                Render3D.renderBlock(event.ctx, node.mainPos, color, outline, fill, phase)

                if (showText.value) {
                    Render3D.renderString(assignedClass.label(), Vec3.atCenterOf(node.textPos).add(0.0, 1.0, 0.0), phase = phase)
                }
            }
        }
    }

    private fun DungeonClass.label() = when (this) {
        DungeonClass.Mage -> "§7[§bMage§7]"
        DungeonClass.Archer -> "§7[§4Archer§7]"
        DungeonClass.Tank -> "§7[§2Tank§7]"
        DungeonClass.Healer -> "§7[§5Healer§7]"
        DungeonClass.Berserk -> "§7[§6Berserk§7]"
        else -> "§7[§8???§7]"
    }
}
