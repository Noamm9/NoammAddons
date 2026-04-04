package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.dungeons.DungeonProfileSummary
import com.github.noamm9.utils.dungeons.enums.DungeonClass

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

object PartyHudFormatter {
    fun buildRows(
        members: Iterable<PartyHudMember>,
        config: PartyHudDisplayConfig = PartyHudDisplayConfig(),
        summaryLookup: (String) -> DungeonProfileSummary?
    ): List<String> {
        return members.map { formatRow(it, summaryLookup(it.name), config) }
    }

    fun formatRow(
        member: PartyHudMember,
        summary: DungeonProfileSummary?,
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
        val personalBestText = summary?.floorPbMilliseconds?.let(PartyFinderRules::formatDuration) ?: "?"

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

    fun previewRows(config: PartyHudDisplayConfig = PartyHudDisplayConfig()): List<String> {
        val previewData = listOf(
            PartyHudMember("ArcherGuy", DungeonClass.Archer, 50) to DungeonProfileSummary(53, 17_400, 2_900, 6.0, 271_000, DungeonClass.Archer, 50, DungeonClass.Archer, 50),
            PartyHudMember("MageMain", DungeonClass.Mage, 49) to DungeonProfileSummary(52, 24_300, 3_000, 8.1, 274_000, DungeonClass.Mage, 49, DungeonClass.Mage, 49),
            PartyHudMember("HealBot", DungeonClass.Healer, 44) to DungeonProfileSummary(48, 12_600, 2_400, 5.25, 332_000, DungeonClass.Healer, 44, DungeonClass.Healer, 44),
            PartyHudMember("Tanky", DungeonClass.Tank, 41) to DungeonProfileSummary(47, 11_200, 2_800, 4.0, 359_000, DungeonClass.Tank, 41, DungeonClass.Tank, 41),
            PartyHudMember("Bers", DungeonClass.Berserk, 46) to DungeonProfileSummary(50, 16_500, 2_750, 6.0, 287_000, DungeonClass.Berserk, 46, DungeonClass.Berserk, 46),
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
}
