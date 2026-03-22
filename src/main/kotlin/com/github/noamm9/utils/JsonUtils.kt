package com.github.noamm9.utils

import com.github.noamm9.NoammAddons
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kotlinx.serialization.json.*
import kotlinx.serialization.json.Json.Default.parseToJsonElement
import net.minecraft.core.BlockPos
import java.awt.Color
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.UnsupportedEncodingException


object JsonUtils {
    private val colorAdapter = object: TypeAdapter<Color>() {
        override fun write(out: JsonWriter, value: Color?) {
            out.value(value?.rgb)
        }

        override fun read(input: JsonReader): Color {
            return Color(input.nextInt(), true)
        }
    }

    private val blockPosAdapter = object: TypeAdapter<BlockPos>() {
        override fun write(out: JsonWriter, value: BlockPos?) {
            if (value == null) {
                out.nullValue()
                return
            }
            out.beginObject()
            out.name("x").value(value.x)
            out.name("y").value(value.y)
            out.name("z").value(value.z)
            out.endObject()
        }

        override fun read(input: JsonReader): BlockPos? {
            if (input.peek() == JsonToken.NULL) {
                input.nextNull()
                return null
            }

            var x = 0
            var y = 0
            var z = 0

            input.beginObject()
            while (input.hasNext()) {
                when (input.nextName()) {
                    "x" -> x = input.nextInt()
                    "y" -> y = input.nextInt()
                    "z" -> z = input.nextInt()
                    else -> input.skipValue()
                }
            }
            input.endObject()
            return BlockPos(x, y, z)
        }
    }

    val json = Json { ignoreUnknownKeys = true }
    val gsonBuilder = GsonBuilder().setPrettyPrinting()
        .registerTypeAdapter(Color::class.java, colorAdapter)
        .registerTypeAdapter(BlockPos::class.java, blockPosAdapter)
        .create()

    fun JsonObject.getObj(key: String) = this[key]?.jsonObject
    fun JsonObject.getArray(key: String) = this[key]?.jsonArray
    fun JsonObject.getString(key: String) = this[key]?.jsonPrimitive?.content
    fun JsonObject.getInt(key: String) = this[key]?.jsonPrimitive?.int
    fun JsonObject.getDouble(key: String) = this[key]?.jsonPrimitive?.double
    fun JsonObject.getBoolean(key: String) = this[key]?.jsonPrimitive?.boolean


    fun stringToJson(s: String): JsonElement {
        return try {
            parseToJsonElement(s)
        }
        catch (e: Exception) {
            e.printStackTrace()
            throw UnsupportedEncodingException("Failed to parse JSON: ${e.message}")
        }
    }

    fun <T> fromJson(file: File, clazz: Class<T>): T? {
        return try {
            FileReader(file).use { reader -> this.gsonBuilder.fromJson(reader, clazz) }
        }
        catch (e: Exception) {
            NoammAddons.logger.error("[JsonUtils] Failed to parse JSON: Type: ${clazz.javaClass.simpleName} ${e.message}")
            e.printStackTrace()
            null
        }
    }

    fun toJson(file: File, data: Any) {
        FileWriter(file).use { writer ->
            this.gsonBuilder.toJson(data, writer)
        }
    }
}