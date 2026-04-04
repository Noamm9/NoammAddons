package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.MouseClickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.section
import com.github.noamm9.ui.clickgui.components.showIf
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.ui.hud.HudElement
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.PartyUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.DungeonProfileSummaryProvider
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.WorldType
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import org.lwjgl.glfw.GLFW
import java.awt.Color

object PartyHud: Feature(
    name = "Party HUD",
    description = "Displays party dungeon stats including class, cata, secrets, and PB."
) {
    private data class KickButtonHitbox(
        val playerName: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
    )

    private val kickButtonHitboxes = mutableListOf<KickButtonHitbox>()
    private val kickColumnWidth = 8f
    private val horizontalPadding = 4f
    private val verticalPadding = 3f

    private val showInDungeons by ToggleSetting("Show In Dungeons", true)
        .withDescription("Allows Party HUD to render while you are inside dungeons.")
        .section("Behavior")
    private val showOutsideDungeons by ToggleSetting("Show Outside Dungeons", true)
        .withDescription("Allows Party HUD to render outside active dungeons using party fallback data.")
    private val showOnlyInDhub by ToggleSetting("Show Only In DHub", false)
        .withDescription("Restricts outside-dungeon Party HUD rendering to Dungeon Hub only.")
        .showIf { showOutsideDungeons.value }
    private val includeSelf by ToggleSetting("Include Self", true)
        .withDescription("Includes your own player row in the Party HUD.")

    private val showClassName by ToggleSetting("Class", true)
        .withDescription("Displays the dungeon class name.")
        .section("Columns")
    private val showClassLevel by ToggleSetting("Class Level", true)
        .withDescription("Displays the dungeon class level.")
    private val showSecretsStats by ToggleSetting("Secrets Stats", true)
        .withDescription("Displays [total secrets/secrets per run].")
    private val showCatacombsLevel by ToggleSetting("Catacombs Level", true)
        .withDescription("Displays catacombs level.")
    private val showPersonalBest by ToggleSetting("Personal Best", true)
        .withDescription("Displays the selected-floor S+ PB using the configured floor and mode.")
    private val pbFloorSetting by DropdownSetting("PB Floor", 6, listOf("F1", "F2", "F3", "F4", "F5", "F6", "F7"))
        .withDescription("Selects which floor Party HUD uses for PB lookup.")
        .showIf { showPersonalBest.value }
    private val pbMasterMode by ToggleSetting("Master Mode", false)
        .withDescription("Uses master mode PBs instead of regular-floor PBs.")
        .showIf { showPersonalBest.value }

    private val backgroundColor by ColorSetting("Party Hud Background Color", Color(255, 255, 255, 50), true)
        .section("Style")
    private val borderColor by ColorSetting("Party Hud Border Color", Color(255, 255, 255), true)
    private val borderThickness by SliderSetting("Border Thickness", 1, 1, 5, 1)

    private val partyHudElement = object: HudElement() {
        override val name = "Party HUD"
        override val toggle get() = this@PartyHud.enabled
        override val shouldDraw get() = mc.player != null && hudMembers().isNotEmpty()

        override fun draw(ctx: net.minecraft.client.gui.GuiGraphics, example: Boolean): Pair<Float, Float> {
            kickButtonHitboxes.clear()
            val displayConfig = displayConfig()
            val showKickButtons = !example && canClickKickButtons()

            if (example) {
                val rows = PartyHudFormatter.previewRows(displayConfig)
                if (rows.isEmpty()) return 0f to 0f

                return drawRows(ctx, rows, showKickButtons = false, members = null)
            }
            val members = hudMembers()
            if (members.isEmpty()) return 0f to 0f

            val floor = (pbFloorSetting.value + 1).takeIf { showPersonalBest.value }
            val masterMode = pbMasterMode.value
            val rows = members.map { member ->
                val summary = if (displayConfig.usesProfileSummary) {
                    DungeonProfileSummaryProvider.getSummaryOrRequest(member.name, floor, masterMode)
                }
                else null
                PartyHudFormatter.formatRow(member, summary, displayConfig)
            }

            return drawRows(ctx, rows, showKickButtons, members)
        }

        private fun drawRows(
            ctx: net.minecraft.client.gui.GuiGraphics,
            rows: List<String>,
            showKickButtons: Boolean,
            members: List<PartyHudMember>?,
        ): Pair<Float, Float> {
            if (rows.isEmpty()) return 0f to 0f

            val contentStartX = horizontalPadding
            val textOffsetX = contentStartX + if (showKickButtons) kickColumnWidth else 0f
            val maxTextWidth = rows.maxOf { it.width().toFloat() }
            val totalWidth = textOffsetX + maxTextWidth + horizontalPadding
            val totalHeight = (rows.size * 9f) + (verticalPadding * 2f)

            Render2D.drawRect(ctx, 0f, 0f, totalWidth, totalHeight, backgroundColor.value)
            Render2D.drawBorder(ctx, 0f, 0f, totalWidth, totalHeight, borderColor.value, borderThickness.value)

            rows.forEachIndexed { index, row ->
                val rowY = verticalPadding + (index * 9f)

                if (showKickButtons && members != null) {
                    val member = members[index]
                    if (!member.name.equals(mc.user.name, ignoreCase = true)) {
                        Render2D.drawString(ctx, "&cX", contentStartX, rowY)
                        kickButtonHitboxes.add(
                            KickButtonHitbox(
                                playerName = member.name,
                                x = x + contentStartX * scale,
                                y = y + rowY * scale,
                                width = kickColumnWidth * scale,
                                height = 9f * scale,
                            )
                        )
                    }
                }

                Render2D.drawString(ctx, row, textOffsetX, rowY)
            }

            return totalWidth to totalHeight
        }
    }.also(hudElements::add)

    private fun hudMembers(): List<PartyHudMember> {
        val selfName = mc.player?.name?.string
        if (LocationUtils.inDungeon) {
            if (!showInDungeons.value) return emptyList()

            if (DungeonListener.dungeonTeammates.isNotEmpty()) {
                return DungeonListener.dungeonTeammates
                    .asSequence()
                    .filter { includeSelf.value || it.name != selfName }
                    .map { PartyHudMember(it.name, it.clazz, it.clazzLvl) }
                    .toList()
            }
        }

        if (!showOutsideDungeons.value) {
            return emptyList()
        }
        if (showOnlyInDhub.value && LocationUtils.world != WorldType.DungeonHub) {
            return emptyList()
        }

        val orderedMembers = LinkedHashSet<String>()
        PartyUtils.members.forEach(orderedMembers::add)
        if (includeSelf.value) {
            selfName?.let(orderedMembers::add)
        }

        return orderedMembers.map { playerName ->
            PartyUtils.getDungeonMemberInfo(playerName)?.let { info ->
                PartyHudMember(playerName, info.dungeonClass, info.classLevel)
            } ?: PartyHudMember(playerName, DungeonClass.Empty, null)
        }
    }

    private fun displayConfig() = PartyHudDisplayConfig(
        showClassName = showClassName.value,
        showClassLevel = showClassLevel.value,
        showSecretsStats = showSecretsStats.value,
        showCatacombsLevel = showCatacombsLevel.value,
        showPersonalBest = showPersonalBest.value,
    )

    override fun init() {
        register<MouseClickEvent> {
            if (event.button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return@register
            if (event.action != GLFW.GLFW_PRESS) return@register
            if (!canClickKickButtons()) return@register
            if (kickButtonHitboxes.isEmpty()) return@register

            Resolution.refresh()
            val mouseX = Resolution.getMouseX()
            val mouseY = Resolution.getMouseY()
            val hitbox = kickButtonHitboxes.firstOrNull {
                mouseX >= it.x && mouseX <= it.x + it.width &&
                    mouseY >= it.y && mouseY <= it.y + it.height
            } ?: return@register

            ChatUtils.sendCommand("party kick ${hitbox.playerName}")
            event.isCanceled = true
        }
    }

    override fun onDisable() {
        kickButtonHitboxes.clear()
        super.onDisable()
    }

    private fun canClickKickButtons(): Boolean {
        if (!enabled) return false
        if (mc.player == null || hudMembers().isEmpty()) return false
        if (!PartyUtils.isLeader()) return false
        return when (mc.screen) {
            is ChatScreen -> true
            is AbstractContainerScreen<*> -> true
            else -> false
        }
    }
}
