package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.NoammAddons
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
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.JsonUtils.getDouble
import com.github.noamm9.utils.JsonUtils.getInt
import com.github.noamm9.utils.JsonUtils.getObj
import com.github.noamm9.utils.JsonUtils.getString
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.PartyUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.WorldType
import com.github.noamm9.utils.network.ApiUtils
import com.github.noamm9.utils.network.ProfileUtils
import com.github.noamm9.utils.network.cache.ProfileCache
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import org.lwjgl.glfw.GLFW
import java.awt.Color
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class PartyHudMember(
    val name: String,
    val dungeonClass: DungeonClass,
    val classLevel: Int?,
)

data class PartyHudDisplayConfig(
    val showClassName: Boolean = true,
    val showClassLevel: Boolean = true,
    val showSecretsStats: Boolean = true,
    val showCatacombsLevel: Boolean = true,
    val showPersonalBest: Boolean = true,
) {
    val usesProfileSummary: Boolean
        get() = showClassName || showClassLevel || showSecretsStats || showCatacombsLevel || showPersonalBest
}

data class PartyHudProfileSummary(
    val catacombsLevel: Int?,
    val totalSecrets: Int?,
    val totalRuns: Int?,
    val secretsPerRun: Double?,
    val floorPbMilliseconds: Int?,
    val selectedClass: DungeonClass?,
    val selectedClassLevel: Int?,
    val bestClass: DungeonClass?,
    val bestClassLevel: Int?,
)

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
    private val pendingProfiles = ConcurrentHashMap<String, Deferred<Result<JsonObject>>>()
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
                val rows = previewRows(displayConfig)
                if (rows.isEmpty()) return 0f to 0f

                return drawRows(ctx, rows, showKickButtons = false, members = null)
            }
            val members = hudMembers()
            if (members.isEmpty()) return 0f to 0f

            val floor = (pbFloorSetting.value + 1).takeIf { showPersonalBest.value }
            val masterMode = pbMasterMode.value
            val rows = members.map { member ->
                val summary = if (displayConfig.usesProfileSummary) getSummaryOrRequest(member.name, floor, masterMode)
                else null
                formatRow(member, summary, displayConfig)
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
            PartyHudMember(playerName, DungeonClass.Empty, null)
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

    private fun getSummaryOrRequest(playerName: String, floor: Int? = null, masterMode: Boolean = false): PartyHudProfileSummary? {
        return getCachedSummary(playerName, floor, masterMode) ?: run {
            requestProfile(playerName)
            null
        }
    }

    private fun getCachedSummary(playerName: String, floor: Int? = null, masterMode: Boolean = false): PartyHudProfileSummary? {
        val key = cacheKey(playerName)
        val profile = ProfileCache.getFromCache(key) ?: return null
        return summarize(profile, floor, masterMode)
    }

    private fun requestProfile(playerName: String): Deferred<Result<JsonObject>> {
        val cleanName = cleanName(playerName)
        val key = cleanName.lowercase()

        ProfileCache.getFromCache(key)?.let { return CompletableDeferred(Result.success(it)) }

        return pendingProfiles.computeIfAbsent(key) {
            NoammAddons.scope.async {
                try {
                    ProfileUtils.getProfile(cleanName)
                }
                finally {
                    pendingProfiles.remove(key)
                }
            }
        }
    }

    private fun summarize(profile: JsonObject, floor: Int? = null, masterMode: Boolean = false): PartyHudProfileSummary {
        val dungeons = profile.getObj("dungeons")
        val totalSecrets = dungeons?.getInt("secrets")
        val totalRuns = extractTotalRuns(dungeons)
        val selectedMode = if (masterMode) dungeons?.getObj("master_catacombs") else dungeons?.getObj("catacombs")
        val classLevels = extractClassLevels(dungeons)
        val selectedClass = dungeons?.getString("selected_dungeon_class")
            ?.let(DungeonClass::fromName)
            ?.takeUnless { it == DungeonClass.Empty }
        val bestClassEntry = classLevels.maxByOrNull { it.value }

        return PartyHudProfileSummary(
            catacombsLevel = dungeons?.getDouble("catacombs_experience")?.let(ApiUtils::getCatacombsLevel),
            totalSecrets = totalSecrets,
            totalRuns = totalRuns,
            secretsPerRun = when {
                totalRuns == null -> null
                totalRuns == 0 -> 0.0
                totalSecrets == null -> null
                else -> totalSecrets.toDouble() / totalRuns.toDouble()
            },
            floorPbMilliseconds = floor?.takeIf { it > 0 }
                ?.let { selectedMode?.getObj("fastest_time_s_plus")?.getInt("$it") },
            selectedClass = selectedClass,
            selectedClassLevel = selectedClass?.let(classLevels::get),
            bestClass = bestClassEntry?.key,
            bestClassLevel = bestClassEntry?.value,
        )
    }

    private fun cacheKey(playerName: String) = cleanName(playerName).lowercase()

    private fun cleanName(playerName: String) = playerName.removeFormatting()

    private fun extractTotalRuns(dungeons: JsonObject?): Int? {
        if (dungeons == null) return null

        val normalRuns = extractTierCompletionTotal(dungeons.getObj("catacombs")?.getObj("tier_completions"))
        val masterRuns = extractTierCompletionTotal(dungeons.getObj("master_catacombs")?.getObj("tier_completions"))

        return if (normalRuns != null || masterRuns != null) {
            (normalRuns ?: 0) + (masterRuns ?: 0)
        }
        else dungeons.getInt("total_runs")
    }

    private fun extractTierCompletionTotal(completions: JsonObject?): Int? {
        if (completions == null) return null

        completions.getInt("total")?.let { return it }

        val perFloorValues = completions.entries
            .mapNotNull { (key, value) -> key.toIntOrNull()?.let { value.jsonPrimitive.content.toIntOrNull() } }

        return perFloorValues.takeIf { it.isNotEmpty() }?.sum()
    }

    private fun extractClassLevels(dungeons: JsonObject?): Map<DungeonClass, Int> {
        if (dungeons == null) return emptyMap()

        val playerClasses = dungeons.getObj("player_classes")

        return listOf(
            DungeonClass.Archer to "archer",
            DungeonClass.Berserk to "berserk",
            DungeonClass.Healer to "healer",
            DungeonClass.Mage to "mage",
            DungeonClass.Tank to "tank",
        ).mapNotNull { (dungeonClass, key) ->
            val experience = playerClasses?.getDouble(key)
                ?: playerClasses?.getObj(key)?.getDouble("experience")
                ?: dungeons.getDouble("${key}_experience")
                ?: return@mapNotNull null
            dungeonClass to ApiUtils.getCatacombsLevel(experience)
        }.toMap()
    }

    private fun formatRow(
        member: PartyHudMember,
        summary: PartyHudProfileSummary?,
        config: PartyHudDisplayConfig = PartyHudDisplayConfig(),
    ): String {
        val resolvedClass = member.dungeonClass.takeUnless { it == DungeonClass.Empty }
            ?: summary?.selectedClass
            ?: summary?.bestClass
            ?: DungeonClass.Empty
        val classLevel = member.classLevel
            ?: summary?.let {
                when (resolvedClass) {
                    it.selectedClass -> it.selectedClassLevel
                    it.bestClass -> it.bestClassLevel
                    else -> null
                }
            }
        val classColor = classColor(resolvedClass)
        val classNameText = className(resolvedClass)
        val classLevelText = classLevel?.toString() ?: "?"
        val catacombsLevelText = summary?.catacombsLevel?.toString() ?: "?"
        val totalSecretsText = summary?.totalSecrets?.toString() ?: "?"
        val secretsPerRunText = summary?.secretsPerRun?.toFixed(2) ?: "?"
        val personalBestText = summary?.floorPbMilliseconds?.let(::formatDuration) ?: "?"

        val detailSegments = buildList {
            val classSegments = buildList {
                if (config.showClassName) add(classNameText)
                if (config.showClassLevel) add(classLevelText)
            }
            if (classSegments.isNotEmpty()) {
                add("$classColor${classSegments.joinToString(" ")}&r")
            }

            if (config.showCatacombsLevel) {
                add("&e($catacombsLevelText)&r")
            }

            if (config.showSecretsStats) {
                add("&a[$totalSecretsText/$secretsPerRunText]&r")
            }

            if (config.showPersonalBest) {
                add("&9[$personalBestText]&r")
            }
        }

        val prefix = "${classColor}${member.name}&r:"
        return if (detailSegments.isEmpty()) prefix
        else "$prefix  ${detailSegments.joinToString(" ")}"
    }

    private fun previewRows(config: PartyHudDisplayConfig = PartyHudDisplayConfig()): List<String> {
        val previewData = listOf(
            PartyHudMember("ArcherGuy", DungeonClass.Archer, 50) to PartyHudProfileSummary(53, 17_400, 2_900, 6.0, 271_000, DungeonClass.Archer, 50, DungeonClass.Archer, 50),
            PartyHudMember("MageMain", DungeonClass.Mage, 49) to PartyHudProfileSummary(52, 24_300, 3_000, 8.1, 274_000, DungeonClass.Mage, 49, DungeonClass.Mage, 49),
            PartyHudMember("HealBot", DungeonClass.Healer, 44) to PartyHudProfileSummary(48, 12_600, 2_400, 5.25, 332_000, DungeonClass.Healer, 44, DungeonClass.Healer, 44),
            PartyHudMember("Tanky", DungeonClass.Tank, 41) to PartyHudProfileSummary(47, 11_200, 2_800, 4.0, 359_000, DungeonClass.Tank, 41, DungeonClass.Tank, 41),
            PartyHudMember("Bers", DungeonClass.Berserk, 46) to PartyHudProfileSummary(50, 16_500, 2_750, 6.0, 287_000, DungeonClass.Berserk, 46, DungeonClass.Berserk, 46),
        )

        return previewData.map { (member, summary) -> formatRow(member, summary, config) }
    }

    private fun className(dungeonClass: DungeonClass) = dungeonClass
        .takeUnless { it == DungeonClass.Empty }
        ?.name
        ?: "?"

    private fun classColor(dungeonClass: DungeonClass) = when (dungeonClass) {
        DungeonClass.Archer -> "&4"
        DungeonClass.Mage -> "&3"
        DungeonClass.Healer -> "&5"
        DungeonClass.Tank -> "&2"
        DungeonClass.Berserk -> "&6"
        DungeonClass.Empty -> "&7"
    }

    private fun formatDuration(milliseconds: Number): String {
        val totalSeconds = milliseconds.toLong() / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        }
        else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }
}

