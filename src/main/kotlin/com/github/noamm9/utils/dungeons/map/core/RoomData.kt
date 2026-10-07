package com.github.noamm9.utils.dungeons.map.core

import com.google.common.reflect.TypeToken
import com.google.gson.*
import net.minecraft.core.BlockPos
import java.lang.reflect.Type

data class RoomData(
    val name: String,
    val type: RoomType,
    val shape: RoomShape,
    val cores: List<Int>,
    val secretCoords: SecretCoords = SecretCoords(),
    val trappedChests: Int = 0,
    val reviveStones: Int = 0,
    val secrets: Int = 0,
    val crypts: Int = 0,
) {
    data class SecretCoords(
        val redstoneKey: List<BlockPos> = emptyList(),
        val wither: List<BlockPos> = emptyList(),
        val bat: List<BlockPos> = emptyList(),
        val item: List<BlockPos> = emptyList(),
        val chest: List<BlockPos> = emptyList()
    )

    fun isUnknown() = name == "Unknown" && shape == RoomShape.UNKNOWN

    companion object {
        fun createUnknown(type: RoomType) = RoomData("Unknown", type, RoomShape.UNKNOWN, emptyList())

        class Deserializer: JsonDeserializer<RoomData> {
            override fun deserialize(json: JsonElement, typeOfT: Type, ctx: JsonDeserializationContext): RoomData {
                val obj = json.asJsonObject
                return RoomData(
                    name = obj.get("name").asString,
                    type = ctx.deserialize(obj.get("type"), RoomType::class.java),
                    shape = ctx.deserialize(obj.get("shape"), RoomShape::class.java),
                    cores = ctx.deserialize(obj.get("cores"), object: TypeToken<List<Int>>() {}.type),
                    secretCoords = obj.get("secretCoords")?.takeUnless(JsonElement::isJsonNull)?.let {
                        ctx.deserialize(it, SecretCoords::class.java)
                    } ?: SecretCoords(),
                    trappedChests = obj.get("trappedChests")?.takeUnless(JsonElement::isJsonNull)?.asInt ?: 0,
                    reviveStones = obj.get("reviveStones")?.takeUnless(JsonElement::isJsonNull)?.asInt ?: 0,
                    secrets = obj.get("secrets")?.takeUnless(JsonElement::isJsonNull)?.asInt ?: 0,
                    crypts = obj.get("crypts")?.takeUnless(JsonElement::isJsonNull)?.asInt ?: 0,
                )
            }
        }

        class SecretCoordsDeserializer: JsonDeserializer<SecretCoords> {
            override fun deserialize(json: JsonElement, typeOfT: Type, ctx: JsonDeserializationContext): SecretCoords {
                val obj = json.asJsonObject
                fun coords(key: String) = obj.get(key)?.takeUnless(JsonElement::isJsonNull)?.let {
                    ctx.deserialize<List<BlockPos>>(it, object: TypeToken<List<BlockPos>>() {}.type)
                } ?: emptyList()
                return SecretCoords(
                    redstoneKey = coords("redstoneKey"),
                    wither = coords("wither"),
                    bat = coords("bat"),
                    item = coords("item"),
                    chest = coords("chest"),
                )
            }
        }
    }
}