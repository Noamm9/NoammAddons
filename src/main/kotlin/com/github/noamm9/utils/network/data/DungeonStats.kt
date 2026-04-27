package com.github.noamm9.utils.network.data

import com.github.noamm9.utils.network.ApiUtils
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DungeonStats(
    @SerialName("favorite_arrow") val favoriteArrow: String,
    @SerialName("selected_power") val selectedPower: String,
    @SerialName("abiphone_contacts") val abiphoneContacts: List<String>,
    @SerialName("consumed_rift_prism") val consumedRiftPrism: Boolean,
    @SerialName("blood_mobs_killed") val bloodMobsKilled: Int,
    val dungeons: DungeonData,
    @SerialName("active_pet") val activePet: PetSummary?,
    val pets: List<PetSummary>,
    @SerialName("armor_data") val armorData: String,
    @SerialName("talisman_bag_data") val talismanBagData: String
) {
    val armorInv by lazy { ApiUtils.decodeBase64ItemList(armorData) }
    val magicalPower by lazy { ApiUtils.getMagicalPower(ApiUtils.decodeBase64ItemList(talismanBagData), this) }
    val cataLevel by lazy { ApiUtils.getCatacombsLevel(dungeons.catacombsExperience) }
    val classAverage by lazy { dungeons.playerClasses.values.map(ApiUtils::getCatacombsLevel).average() }
    val secretAverage by lazy { dungeons.secrets.toDouble() / dungeons.totalRuns.toDouble() }
}

@Serializable
data class DungeonData(
    @SerialName("total_runs") val totalRuns: Int,
    val secrets: Int,
    @SerialName("player_classes") val playerClasses: Map<String, Double>,
    @SerialName("catacombs_experience") val catacombsExperience: Double,
    val catacombs: TierData,
    @SerialName("master_catacombs") val masterCatacombs: TierData
)

@Serializable
data class TierData(
    @SerialName("tier_completions") val tierCompletions: Map<String, Int>,
    @SerialName("fastest_time_s_plus") val fastestTimeSPlus: Map<String, Long>
)

@Serializable
data class PetSummary(val type: String, val tier: String)