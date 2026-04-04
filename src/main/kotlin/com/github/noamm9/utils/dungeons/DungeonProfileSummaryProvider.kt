package com.github.noamm9.utils.dungeons

import com.github.noamm9.NoammAddons
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.JsonUtils.getDouble
import com.github.noamm9.utils.JsonUtils.getInt
import com.github.noamm9.utils.JsonUtils.getObj
import com.github.noamm9.utils.JsonUtils.getString
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.network.ApiUtils
import com.github.noamm9.utils.network.ProfileUtils
import com.github.noamm9.utils.network.cache.ProfileCache
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.ConcurrentHashMap

data class DungeonProfileSummary(
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

object DungeonProfileSummaryProvider {
    private val pendingProfiles = ConcurrentHashMap<String, Deferred<Result<JsonObject>>>()

    fun summarize(profile: JsonObject, floor: Int? = null, masterMode: Boolean = false): DungeonProfileSummary {
        val dungeons = profile.getObj("dungeons")
        val totalSecrets = dungeons?.getInt("secrets")
        val totalRuns = extractTotalRuns(dungeons)
        val selectedMode = if (masterMode) dungeons?.getObj("master_catacombs") else dungeons?.getObj("catacombs")
        val classLevels = extractClassLevels(dungeons)
        val selectedClass = dungeons?.getString("selected_dungeon_class")
            ?.let(DungeonClass::fromName)
            ?.takeUnless { it == DungeonClass.Empty }
        val bestClassEntry = classLevels.maxByOrNull { it.value }

        return DungeonProfileSummary(
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

    fun getCachedSummary(playerName: String, floor: Int? = null, masterMode: Boolean = false): DungeonProfileSummary? {
        val key = cacheKey(playerName)
        val profile = ProfileCache.getFromCache(key) ?: return null
        return summarize(profile, floor, masterMode)
    }

    fun getSummaryOrRequest(playerName: String, floor: Int? = null, masterMode: Boolean = false): DungeonProfileSummary? {
        return getCachedSummary(playerName, floor, masterMode) ?: run {
            requestProfile(playerName)
            null
        }
    }

    suspend fun loadSummary(playerName: String, floor: Int? = null, masterMode: Boolean = false): DungeonProfileSummary? {
        return loadProfile(playerName).getOrNull()?.let { summarize(it, floor, masterMode) }
    }

    private suspend fun loadProfile(playerName: String): Result<JsonObject> {
        val key = cacheKey(playerName)
        ProfileCache.getFromCache(key)?.let { return Result.success(it) }
        return pendingProfiles[key]?.await() ?: requestProfile(playerName).await()
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
}
