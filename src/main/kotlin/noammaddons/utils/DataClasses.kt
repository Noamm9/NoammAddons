package noammaddons.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import net.minecraft.client.entity.EntityOtherPlayerMP
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth

class DataClasses {
    @Serializable
    data class APIMayor(
        @SerialName("name")
        val name: String? = null,
        @SerialName("perks")
        val perks: List<Map<String, String>>? = null
    )

    data class Mayor(
        val name: String? = null,
        val perks: List<String>? = null
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

    class HudElementConfig {
        val MaskTimers = HudElementData(100f, 10f, 1f)
        val GhostPick = HudElementData(100f, 40f, 1f)
        val ClockDisplay = HudElementData(100f, 50f, 1f)
        val FPSdisplay = HudElementData(100f, 60f, 1f)
        val WitherShieldTimer = HudElementData(100f, 70f, 2f)
        val SpringBootsDisplay = HudElementData(100f, 80f, 4f)
        val PlayerHud = PlayerHudData(
            health = HudElementData(100f, 90f, 1f),
            mana = HudElementData(100f, 100f, 1f),
            overflowMana = HudElementData(100f, 110f, 1f),
            defense = HudElementData(100f, 120f, 1f),
            effectiveHP = HudElementData(100f, 130f, 1f),
            speed = HudElementData(100f, 140f, 1f)
        )
        val PetDisplay = HudElementData(100f, 150f, 1f)
        val TpsDisplay = HudElementData(100f, 160f, 1f)
        val CustomScoreBoard = HudElementData(mc.getWidth() * 1f, mc.getHeight() / 2f, 3f)
        val SecretDisplay = HudElementData(130f, 130f, 1f)
        val dungeonMap = HudElementData(100f, 100f, 1f)
        val scoreCalculator = HudElementData(100f, 100f, 1f) // todo
        val dungeonWarpCooldown = HudElementData(100f, 100f, 1f)
    }

    data class HudElementData(var x: Float, var y: Float, var scale: Float)

    data class PlayerHudData(
        var health: HudElementData,
        var defense: HudElementData,
        var effectiveHP: HudElementData,
        var mana: HudElementData,
        var overflowMana: HudElementData,
        var speed: HudElementData
    )

    class PersonalBestData {
        val relics = mutableMapOf<String, Double?>(
            "Red" to null,
            "Orange" to null,
            "Green" to null,
            "Blue" to null,
            "Purple" to null
        )

        val pazzles = mutableMapOf<String, Double?>(
            "Creeper Beams" to null,
            "Blaze" to null,
            "Three Weirdos" to null,
            "Boulder" to null,
            "Purple" to null
        )

        val crystals: Double? = null
    }

    // todo use JsonUtils's get function
    data class Release(
        val html_url: String,
        val id: Int,
        val tag_name: String,
        val name: String?,
        val body: String?,
        val draft: Boolean,
        val prerelease: Boolean,
        val created_at: String,
        val published_at: String,
        val author: Author,
        val assets: List<Asset>
    )

    data class Author(val login: String, val id: Int, val avatar_url: String)

    data class Asset(
        val id: Int,
        val name: String,
        val label: String?,
        val content_type: String,
        val size: Int,
        val download_count: Int,
        val browser_download_url: String
    )

    data class PlayerData(
        var name: String = "",
        var rank: String = "",
        var entityOtherPlayerMP: EntityOtherPlayerMP? = null,
        var skyCryptData: JsonObject? = null,
        var lilyWeight: JsonObject? = null
    )


}