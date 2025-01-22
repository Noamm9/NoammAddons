package noammaddons.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object DataClasses {
    @Serializable
    data class Mayor(
        @SerialName("name")
        val name: String? = null,
        @SerialName("perks")
        val perks: List<Map<String, String>>? = null
    )

    @Serializable
    data class APISBItem(
        @SerialName("id")
        val id: String,
        @SerialName("material")
        val material: String,
        @SerialName("motes_sell_price")
        val motesSellPrice: Double? = null,
        @SerialName("name")
        val name: String,
        @SerialName("npc_sell_price")
        val npcSellPrice: Double? = null,
    )

    data class bzitem(
        val buyPrice: Double,
        val buyVolume: Double,
        val id: String,
        val name: String,
        val price: Double,
        val sellPrice: Double,
        val sellVolume: Double,
        val tag: String?
    )

    data class Room(
        val id: List<String>?,
        val name: String,
        val type: String,
        val shape: String,
        val doors: String?,
        val secrets: Int,
        val crypts: Int,
        val revive_stones: Int,
        val journals: Int,
        val spiders: Boolean,
        val secret_details: SecretDetails,
        val soul: Boolean,
        val cores: List<Int>,
        val secret_coords: SecretCoords?
    )

    data class SecretDetails(
        val wither: Int,
        val redstone_key: Int,
        val bat: Int,
        val item: Int,
        val chest: Int
    )

    data class SecretCoords(
        val chest: List<List<Int>>?,
        val item: List<List<Int>>?,
        val bat: List<List<Int>>?
    )

    data class Coords2D(
        val x: Int,
        val z: Int
    )
}