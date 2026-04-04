package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.MouseClickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.section
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.ui.hud.HudElement
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.PartyUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.DungeonProfileSummaryProvider
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import org.lwjgl.glfw.GLFW

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

    private val showInDungeons by ToggleSetting("Show In Dungeons", true)
        .withDescription("Allows Party HUD to render while you are inside dungeons.")
        .section("Behavior")
    private val showOutsideDungeons by ToggleSetting("Show Outside Dungeons", true)
        .withDescription("Allows Party HUD to render outside active dungeons using party fallback data.")
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
        .withDescription("Displays the selected-floor S+ PB when the current floor is known.")

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

                var maxWidth = 0f
                rows.forEachIndexed { index, row ->
                    Render2D.drawString(ctx, row, 0f, index * 9f)
                    maxWidth = maxOf(maxWidth, row.width().toFloat())
                }

                return maxWidth to rows.size * 9f
            }

            val members = hudMembers()
            if (members.isEmpty()) return 0f to 0f

            val floor = LocationUtils.dungeonFloorNumber?.takeIf { it > 0 }
            val masterMode = LocationUtils.isMasterMode
            var maxWidth = 0f
            members.forEachIndexed { index, member ->
                val summary = if (displayConfig.usesProfileSummary) {
                    DungeonProfileSummaryProvider.getSummaryOrRequest(member.name, floor, masterMode)
                }
                else null

                val row = PartyHudFormatter.formatRow(member, summary, displayConfig)
                val rowY = index * 9f
                val rowX = if (showKickButtons) kickColumnWidth else 0f

                if (showKickButtons && !member.name.equals(mc.user.name, ignoreCase = true)) {
                    Render2D.drawString(ctx, "&cX", 0f, rowY)
                    kickButtonHitboxes.add(
                        KickButtonHitbox(
                            playerName = member.name,
                            x = x,
                            y = y + rowY * scale,
                            width = kickColumnWidth * scale,
                            height = 9f * scale,
                        )
                    )
                }

                Render2D.drawString(ctx, row, rowX, rowY)
                maxWidth = maxOf(maxWidth, rowX + row.width().toFloat())
            }

            return maxWidth to members.size * 9f
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
