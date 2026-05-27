package com.github.noamm9.utils

import com.google.common.reflect.TypeToken
import com.google.gson.*
import net.minecraft.core.BlockPos
import java.awt.Color
import java.lang.reflect.Type

object GsonUtils {
    val gson = GsonBuilder().setPrettyPrinting()
        .registerTypeAdapter(BlockPos::class.java, BlockPosAdapter)
        .registerTypeAdapter(Color::class.java, ColorAdapter)
        .create()

    object ColorAdapter: JsonSerializer<Color>, JsonDeserializer<Color> {
        override fun serialize(src: Color, type: Type, ctx: JsonSerializationContext) = JsonPrimitive(src.rgb)
        override fun deserialize(json: JsonElement, type: Type, ctx: JsonDeserializationContext) = Color(json.asInt, true)
    }

    object BlockPosAdapter: JsonSerializer<BlockPos>, JsonDeserializer<BlockPos> {
        override fun serialize(src: BlockPos, type: Type, ctx: JsonSerializationContext) = JsonObject().apply {
            addProperty("x", src.x)
            addProperty("y", src.y)
            addProperty("z", src.z)
        }

        override fun deserialize(json: JsonElement, type: Type, ctx: JsonDeserializationContext) = json.asJsonObject.run {
            BlockPos(get("x").asInt, get("y").asInt, get("z").asInt)
        }
    }

    fun encode(obj: Any) = gson.toJson(obj)
    inline fun <reified T> decode(json: String): T = gson.fromJson(json, object: TypeToken<T>() {}.type)
}