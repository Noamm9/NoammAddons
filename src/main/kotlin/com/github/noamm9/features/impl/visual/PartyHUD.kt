package com.github.noamm9.features.impl.visual

import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.PartyUtils
import com.github.noamm9.utils.PartyUtils.OnlineState
import com.github.noamm9.utils.PartyUtils.PartyMember
import com.github.noamm9.utils.PartyUtils.PartyRole
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.width

object PartyHUD: Feature("Displays your current party members, their roles and online state on screen.") {
    private val showInvites by ToggleSetting("Show Pending Invites", true)
    private val hideWhenSolo by ToggleSetting("Hide When Not In Party", false)

    private data class Row(val icon: String, val iconX: Int, val text: String, val textX: Int)

    private var cachedLines: List<Pair<String, String>>? = null
    private var rows = emptyList<Row>()
    private var layoutWidth = 0

    private val exampleMembers = listOf(
        PartyMember("Noamm", PartyRole.LEADER, OnlineState.ONLINE, "MVP++", "§6[MVP§c++§6]"),
        PartyMember("jcnlk", PartyRole.MOD, OnlineState.ONLINE, "MVP+", "§b[MVP§c+§b]"),
        PartyMember("Alex", PartyRole.MEMBER, OnlineState.ONLINE),
        PartyMember("Steve", PartyRole.MEMBER, OnlineState.OFFLINE, "VIP", "§a[VIP]"),
    )

    override fun init() {
        hudElement(
            shouldDraw = { LocationUtils.onHypixel && (! hideWhenSolo.value || PartyUtils.isInParty) }
        ) { ctx, example ->
            val lines = when {
                example -> buildLines(exampleMembers, setOf("verybritish"))
                PartyUtils.isInParty -> buildLines(PartyUtils.partyMembers, PartyUtils.pendingInvites)
                else -> listOf("" to "&cNot in a party!")
            }

            if (lines != cachedLines) updateLayout(lines)

            rows.forEachIndexed { i, row ->
                val y = i * 9 + if (i > 0) 2 else 0
                if (row.icon.isNotEmpty()) ctx.drawString(row.icon, row.iconX, y)
                ctx.drawString(row.text, row.textX, y)
            }

            return@hudElement layoutWidth to rows.size * 9
        }
    }

    private fun updateLayout(lines: List<Pair<String, String>>) {
        val iconWidth = lines.maxOf { it.first.width() }
        val columnX = if (iconWidth > 0) iconWidth + 3 else 0

        rows = lines.map { (icon, text) ->
            if (icon.isEmpty()) Row(icon, 0, text, 0)
            else Row(icon, (iconWidth - icon.width()) / 2, text, columnX)
        }
        layoutWidth = rows.maxOf { it.textX + it.text.width() }
        cachedLines = lines
    }

    private fun buildLines(members: List<PartyMember>, invites: Set<String>) = buildList {
        add("" to "&d&lParty &7(${members.size})")
        members.sortedBy { it.role.ordinal }.forEach { add(it.format()) }
        if (showInvites.value) invites.forEach { add("&8»" to "&7$it &8(invited)") }
    }

    private fun PartyMember.format(): Pair<String, String> {
        val role = when (role) {
            PartyRole.LEADER -> "&6👑"
            PartyRole.MOD -> "&e♦"
            PartyRole.MEMBER -> "&7•"
        }
        val name = rankPrefix?.let { "$it ${this.name}" } ?: "&7${this.name}"
        val state = if (isOnline) "&a●" else "&c●"
        return role to "$name $state"
    }
}