package noammaddons.utils

import com.google.gson.*
import com.google.gson.stream.*
import kotlinx.serialization.json.*
import kotlinx.serialization.json.Json.Default.parseToJsonElement
import kotlinx.serialization.json.JsonObject
import net.minecraft.util.BlockPos
import net.minecraft.util.ResourceLocation
import noammaddons.NoammAddons.Companion.mc
import java.awt.Color
import java.io.*


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


    fun stringToJson(s: String): JsonObject {
        return try {
            parseToJsonElement(s).jsonObject
        }
        catch (e: Exception) {
            e.printStackTrace()
            mc.shutdown()
            throw UnsupportedEncodingException("Failed to parse JSON: ${e.message}")
        }
    }

    fun <T> fromJson(file: File, clazz: Class<T>): T? {
        return try {
            FileReader(file).use { reader -> this.gsonBuilder.fromJson(reader, clazz) }
        }
        catch (e: Exception) {
            println("[PogObject] Failed to parse JSON: Type: ${clazz.javaClass.simpleName} ${e.message}")
            null
        }
    }

    fun toJson(file: File, data: Any) {
        FileWriter(file).use { writer ->
            this.gsonBuilder.toJson(data, writer)
        }
    }

    fun readJsonFile(resourcePath: String): JsonObject? {
        val resourceLocation = ResourceLocation(resourcePath)

        return try {
            mc.resourceManager.getResource(resourceLocation).inputStream.use { inputStream ->
                val reader = InputStreamReader(inputStream)
                Gson().fromJson(reader, JsonObject::class.java)
            }
        }
        catch (e: Exception) {
            println("[PogObject] Failed to read JSON: ${e.message}")
            null
        }
    }
}