package com.github.noamm9.utils

import java.text.NumberFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt

object NumbersUtils {
    private val suffixes = TreeMap<Long, String>().apply {
        this[1000L] = "k"
        this[1000000L] = "m"
        this[1000000000L] = "b"
        this[1000000000000L] = "t"
        this[1000000000000000L] = "p"
        this[1000000000000000000L] = "e"
    }
    @JvmStatic
    fun format(value: Number): String {
        @Suppress("NAME_SHADOWING")
        val value = value.toLong()
        if (value == Long.MIN_VALUE) return format(Long.MIN_VALUE + 1)
        if (value < 0L) return "-" + format(- value)
        if (value < 1000) return value.toString()
        val (divideBy, suffix) = suffixes.floorEntry(value)
        val truncated = value / (divideBy / 10)
        val hasDecimal = truncated < 100 && truncated / 10.0 != (truncated / 10).toDouble()
        return if (hasDecimal) (truncated / 10.0).toString() + suffix else (truncated / 10).toString() + suffix
    }

    @JvmStatic
    fun format(value: String): String = format(value.filter { it.isDigit() }.toLong())

    fun Double.toFixed(precision: Int): String {
        if (this.isNaN()) return toString()
        val scale = 10.0.pow(precision).toInt()
        val rounded = (this * scale).roundToInt().toDouble() / scale
        val parts = rounded.toString().split(".")

        return if (parts.size == 2) {
            val decimals = parts[1].padEnd(precision, '0')
            "${parts[0]}.$decimals"
        }
        else {
            "${parts[0]}." + "0".repeat(precision)
        }
    }

    fun Float.toFixed(precision: Int): String = toDouble().toFixed(precision)

    fun String.toFixed(precision: Int): String = toDoubleOrNull()?.toFixed(precision) ?: this

    fun String.romanToDecimal(): Int {
        var decimal = 0
        var lastNumber = 0
        val romanNumeral = this.uppercase()
        for (x in romanNumeral.length - 1 downTo 0) {
            when (romanNumeral[x]) {
                'M' -> {
                    decimal = processDecimal(1000, lastNumber, decimal)
                    lastNumber = 1000
                }

                'D' -> {
                    decimal = processDecimal(500, lastNumber, decimal)
                    lastNumber = 500
                }

                'C' -> {
                    decimal = processDecimal(100, lastNumber, decimal)
                    lastNumber = 100
                }

                'L' -> {
                    decimal = processDecimal(50, lastNumber, decimal)
                    lastNumber = 50
                }

                'X' -> {
                    decimal = processDecimal(10, lastNumber, decimal)
                    lastNumber = 10
                }

                'V' -> {
                    decimal = processDecimal(5, lastNumber, decimal)
                    lastNumber = 5
                }

                'I' -> {
                    decimal = processDecimal(1, lastNumber, decimal)
                    lastNumber = 1
                }
            }
        }
        return decimal
    }

    operator fun Number.div(number: Number) = this.toDouble() / number.toDouble()
    operator fun Number.times(number: Number) = this.toDouble() * number.toDouble()
    operator fun Number.minus(number: Number) = this.toDouble() - number.toDouble()
    operator fun Number.plus(number: Number) = this.toDouble() + number.toDouble()

    fun formatTime(milliseconds: Number): String {
        val totalSecs = milliseconds.toLong() / 1000
        val h = totalSecs / 3600
        val m = (totalSecs % 3600) / 60
        val s = totalSecs % 60

        return buildList {
            if (h > 0) add("${h}h")
            if (m > 0) add("${m}m")
            if (s > 0) add("${s}s")
        }.joinToString(" ")
    }

    private fun processDecimal(decimal: Int, lastNumber: Int, lastDecimal: Int): Int {
        return if (lastNumber > decimal) lastDecimal - decimal
        else lastDecimal + decimal
    }

    fun formatComma(value: Number?): String {
        return value?.let { NumberFormat.getNumberInstance(Locale.US).format(it) }.orEmpty()
    }

    private fun compactMultiplier(c: Char): Long? = when (c) {
        'k' -> 1_000L
        'm' -> 1_000_000L
        'b' -> 1_000_000_000L
        't' -> 1_000_000_000_000L
        else -> null
    }

    fun parseCompactNumber(value: String): Long? {
        if (value.isBlank()) return null
        val cleanValue = value.lowercase().replace(",", "").trim()
        cleanValue.toLongOrNull()?.let { return it }
        val multiplier = compactMultiplier(cleanValue.lastOrNull() ?: return null) ?: return null
        val number = cleanValue.dropLast(1).toDoubleOrNull() ?: return null
        return (number * multiplier).toLong()
    }

    fun parseCompactNumberDouble(value: String): Double? {
        if (value.isBlank()) return null
        value.toDoubleOrNull()?.let { return it }
        val cleanValue = value.lowercase().replace(",", "")
        val multiplier = compactMultiplier(cleanValue.lastOrNull() ?: return null) ?: return null
        val number = cleanValue.dropLast(1).toDoubleOrNull() ?: return null
        return number * multiplier
    }
}