package com.github.noamm9.utils.dungeons

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import java.awt.Color
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.Base64
import java.util.zip.GZIPInputStream

/**
 * Route format compatible with other mod formats, aka hoedin.
 */
object RouteImportParser {

    class RouteImportException(message: String) : Exception(message)

    data class ParsedWaypoint(
        val x: Int, val y: Int, val z: Int,
        val color: Color,
        val filled: Boolean,
        val outline: Boolean,
        val phase: Boolean,
        val title: String?,
    )

    private val defaultColor = Color(0, 255, 0, 255)

    fun parse(clipboard: String): Map<String, List<ParsedWaypoint>> {
        val trimmed = clipboard.trim()
        if (trimmed.isEmpty()) throw RouteImportException("Clipboard is empty.")
        val json = if (trimmed.startsWith("{")) trimmed else decompress(trimmed)
        return parseJson(json)
    }

    private fun decompress(base64: String): String {
        val bytes = try {
            Base64.getMimeDecoder().decode(base64)
        } catch (e: IllegalArgumentException) {
            throw RouteImportException("Clipboard is neither JSON nor valid Base64 data.")
        }
        return try {
            GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            throw RouteImportException("Failed to decompress route data.")
        }
    }

    private fun parseJson(json: String): Map<String, List<ParsedWaypoint>> {
        val root = try {
            JsonParser.parseString(json)
        } catch (e: JsonSyntaxException) {
            throw RouteImportException("Invalid JSON.")
        }
        if (!root.isJsonObject) throw RouteImportException("Route JSON must map room -> waypoints.")

        val result = LinkedHashMap<String, List<ParsedWaypoint>>()
        for ((roomName, element) in root.asJsonObject.entrySet()) {
            if (!element.isJsonArray) continue
            val list = ArrayList<ParsedWaypoint>()
            for (item in element.asJsonArray) {
                if (!item.isJsonObject) continue
                val o = item.asJsonObject

                val coords = if (o.has("blockPos") && o["blockPos"].isJsonObject) {
                    readBlockPos(o["blockPos"].asJsonObject)
                } else if (o.isPrimitive("x") && o.isPrimitive("y") && o.isPrimitive("z")) {
                    Triple(o["x"].asDouble.toInt(), o["y"].asDouble.toInt(), o["z"].asDouble.toInt())
                } else null

                val (x, y, z) = coords ?: continue

                val color = parseColor(o.get("color")?.takeIf { it.isJsonPrimitive }?.asString)
                val filled = o.get("filled")?.asBoolean ?: false
                val depth = o.get("depth")?.asBoolean ?: false
                val title = o.get("title")?.takeIf { it.isJsonPrimitive }?.asString?.ifBlank { null }

                list.add(
                    ParsedWaypoint(
                        x, y, z, color,
                        filled = filled, outline = !filled, phase = !depth, title = title,
                    )
                )
            }
            result[roomName] = list
        }
        return result
    }

    /** Parses `RRGGBBAA` (or `RRGGBB`), with or without a leading `#`. */
    fun parseColor(hex: String?): Color {
        if (hex == null) return defaultColor
        val h = hex.removePrefix("#")
        if (h.length < 6) return defaultColor
        return try {
            val r = h.substring(0, 2).toInt(16)
            val g = h.substring(2, 4).toInt(16)
            val b = h.substring(4, 6).toInt(16)
            val a = if (h.length >= 8) h.substring(6, 8).toInt(16) else 255
            Color(r, g, b, a)
        } catch (e: NumberFormatException) {
            defaultColor
        }
    }

    private fun JsonObject.isPrimitive(key: String): Boolean = has(key) && this[key].isJsonPrimitive

    /**
     * Reads x/y/z from a serialized block position. Uses readable `x`/`y`/`z` keys when
     * present, otherwise falls back to the three integer values in order (x, y, z) so that
     * obfuscated field names are still handled.
     */
    private fun readBlockPos(b: JsonObject): Triple<Int, Int, Int>? {
        if (b.isPrimitive("x") && b.isPrimitive("y") && b.isPrimitive("z"))
            return Triple(b["x"].asInt, b["y"].asInt, b["z"].asInt)
        val ints = b.entrySet().mapNotNull { (_, v) ->
            if (v.isJsonPrimitive && v.asJsonPrimitive.isNumber) v.asInt else null
        }
        return if (ints.size == 3) Triple(ints[0], ints[1], ints[2]) else null
    }
}
