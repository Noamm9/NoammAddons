package noammaddons.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import net.minecraft.client.entity.EntityOtherPlayerMP
import kotlin.math.*

data class Vector3(var x: Double = 0.0, var y: Double = 0.0, var z: Double = 0.0) {
    companion object {
        fun fromPitchYaw(pitch: Double, yaw: Double): Vector3 {
            val f = cos(- yaw * 0.017453292 - Math.PI)
            val f1 = sin(- yaw * 0.017453292 - Math.PI)
            val f2 = - cos(- pitch * 0.017453292)
            val f3 = sin(- pitch * 0.017453292)
            return Vector3(f1 * f2, f3, f * f2).normalize()
        }
    }

    fun add(other: Vector3): Vector3 {
        this.x += other.x
        this.y += other.y
        this.z += other.z
        return this
    }

    fun addY(value: Double): Vector3 {
        this.y += value
        return this
    }

    fun multiply(factor: Double): Vector3 {
        this.x *= factor
        this.y *= factor
        this.z *= factor
        return this
    }

    fun normalize(): Vector3 {
        val length = length()
        if (length != 0.0) multiply(1.0 / length)
        return this
    }

    fun length(): Double = sqrt(x * x + y * y + z * z)

    fun dotProduct(vector3: Vector3): Double {
        return (x * vector3.x) + (y * vector3.y) + (z * vector3.z)
    }

    fun getAngleRad(vector3: Vector3): Double {
        return acos(dotProduct(vector3) / (length() * vector3.length()))
    }

    fun getAngleDeg(vector3: Vector3): Double {
        return 180 / Math.PI * getAngleRad(vector3)
    }
}


data class ApiMayor(
    val mayor: Candidate,
    val minister: Minister
) {
    companion object {
        val empty = ApiMayor(Candidate("", emptyList()), Minister("", Perk("", "")))
    }

    @Serializable
    data class Candidate(
        @SerialName("name")
        val name: String,
        @SerialName("perks")
        val perks: List<Perk>,
    )

    @Serializable
    data class Minister(
        @SerialName("name")
        val name: String,
        @SerialName("perk")
        val perk: Perk
    )

    @Serializable
    data class Perk(val name: String, val description: String)
}

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

@Serializable
data class bzitem(
    @SerialName("quick_status")
    val quick_status: ApiBzItem,
    @SerialName("buy_summary")
    val buy_summary: List<Map<String, Double>>
)

@Serializable
data class ApiBzItem(
    @SerialName("productId")
    val productId: String,
    @SerialName("sellPrice")
    val sellPrice: Double,
    @SerialName("sellVolume")
    val sellVolume: Double,
    @SerialName("sellMovingWeek")
    val sellMovingWeek: Double,
    @SerialName("sellOrders")
    val sellOrders: Double,
    @SerialName("buyPrice")
    val buyPrice: Double,
    @SerialName("buyVolume")
    val buyVolume: Double,
    @SerialName("buyMovingWeek")
    val buyMovingWeek: Double,
    @SerialName("buyOrders")
    val buyOrders: Double
)

data class PetDisplayData(
    val hudData: HudElementData,
    var pet: String = "                     "
)

class HudElementConfig {
    val maskTimers = HudElementData(100f, 10f, 1f)
    val ghostPick = HudElementData(100f, 40f, 1f)
    val clockDisplay = HudElementData(100f, 50f, 1f)
    val FPSdisplay = HudElementData(100f, 60f, 1f)
    val witherShieldTimer = HudElementData(100f, 70f, 2f)
    val springBootsDisplay = HudElementData(100f, 80f, 4f)
    val playerHud = PlayerHudData(
        health = HudElementData(100f, 90f, 1f),
        mana = HudElementData(100f, 100f, 1f),
        overflowMana = HudElementData(100f, 110f, 1f),
        defense = HudElementData(100f, 120f, 1f),
        effectiveHP = HudElementData(100f, 130f, 1f),
        speed = HudElementData(100f, 140f, 1f)
    )
    val petDisplay = PetDisplayData(HudElementData(100f, 150f, 1f))
    val tpsDisplay = HudElementData(100f, 160f, 1f)
    val customScoreBoard = HudElementData(100f, 100f, 0.7f)
    val secretDisplay = HudElementData(100f, 100f, 1f)
    val dungeonMap = HudElementData(100f, 100f, 1f)
    val scoreCalculator = HudElementData(100f, 100f, 1f)
    val inventoryDisplay = HudElementData(100f, 100f, 1f)
    val dungeonWarpCooldown = HudElementData(100f, 100f, 1f)
    val dungeonRunSplits = HudElementData(100f, 100f, 1f)
    val blessingDisplay = HudElementData(100f, 100f, 1f)
    val chestProfitHud = HudElementData(10f, 10f, 1f)
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
        "Teleport Maze" to null,
        "Water Board" to null,
        "Tic Tac Toe" to null
    )

    val crystals = mutableListOf<Double?>()

    val dungeonSplits = mutableMapOf<String, MutableMap<String, Long>>()
}

data class PlayerData(
    var name: String = "",
    var rank: String = "",
    var entityOtherPlayerMP: EntityOtherPlayerMP? = null,
    var skyCryptData: JsonObject? = null,
    var lilyWeight: JsonObject? = null
)