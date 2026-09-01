package com.github.noamm9.utils

import com.google.common.reflect.TypeToken
import com.google.gson.*
import net.minecraft.core.BlockPos
import java.awt.Color
import java.lang.reflect.Type
import java.util.*
import kotlin.jvm.optionals.getOrNull

object GsonUtils {
    val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().apply {
        registerTypeAdapter(BlockPos::class.java, BlockPosAdapter())
        registerTypeAdapter(Color::class.java, ColorAdapter())
        registerTypeAdapter(Regex::class.java, RegexAdapter())
        registerTypeAdapter(Optional::class.java, OptionalAdapter())
    }.create()

    inline fun <reified T: Any> decode(json: String): T = gson.fromJson(json, object: TypeToken<T>() {}.type)
    fun encode(obj: Any) = gson.toJson(obj)

    inline fun jsonObject(block: JsonObject.() -> Unit) = JsonObject().apply(block)
    inline fun jsonArray(block: JsonArray.() -> Unit) = JsonArray().apply(block)

    class ColorAdapter: JsonSerializer<Color>, JsonDeserializer<Color> {
        override fun serialize(src: Color, type: Type, ctx: JsonSerializationContext) = JsonPrimitive(src.rgb)
        override fun deserialize(json: JsonElement, type: Type, ctx: JsonDeserializationContext) = Color(json.asInt, true)
    }

    class RegexAdapter: JsonSerializer<Regex>, JsonDeserializer<Regex> {
        override fun serialize(src: Regex, type: Type, ctx: JsonSerializationContext) = JsonPrimitive(src.pattern)
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?) = Regex(json !!.asString)
    }

    class BlockPosAdapter: JsonSerializer<BlockPos>, JsonDeserializer<BlockPos> {
        override fun serialize(src: BlockPos, type: Type, ctx: JsonSerializationContext) = JsonObject().apply {
            addProperty("x", src.x)
            addProperty("y", src.y)
            addProperty("z", src.z)
        }

        override fun deserialize(json: JsonElement, type: Type, ctx: JsonDeserializationContext) = json.asJsonObject.run {
            BlockPos(get("x").asInt, get("y").asInt, get("z").asInt)
        }
    }

    class OptionalAdapter: JsonSerializer<Optional<*>>, JsonDeserializer<Optional<*>> {
        override fun serialize(src: Optional<*>, type: Type, ctx: JsonSerializationContext) = JsonPrimitive(src.getOrNull().toString())
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Optional<*> {
            return Optional.ofNullable(json?.asString)
        }
    }
}