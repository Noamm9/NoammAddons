package noammaddons.utils

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
    private val romanSymbols = TreeMap(
        mapOf(
            1000 to "M",
            900 to "CM",
            500 to "D",
            400 to "CD",
            100 to "C",
            90 to "XC",
            50 to "L",
            40 to "XL",
            10 to "X",
            9 to "IX",
            5 to "V",
            4 to "IV",
            1 to "I",
        )
    )

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


    @JvmStatic
    fun unformat(value: String): Long {
        val suffix = value.filter { ! it.isDigit() }.lowercase()
        val num = value.filter { it.isDigit() }.toLong()
        return num * (suffixes.entries.find { it.value.lowercase() == suffix }?.key ?: 1)
    }

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

    fun Float.toFixed(precision: Int): String {
        val scale = 10.0.pow(precision).toInt()
        val rounded = (this * scale).roundToInt().toFloat() / scale
        val parts = rounded.toString().split(".")

        return if (parts.size == 2) {
            val decimals = parts[1].padEnd(precision, '0')
            "${parts[0]}.$decimals"
        }
        else {
            "${parts[0]}." + "0".repeat(precision)
        }
    }

    fun String.toFixed(precision: Int): String {
        val number = toDoubleOrNull() ?: return this
        val scale = 10.0.pow(precision).toInt()
        val rounded = (number * scale).roundToInt().toDouble() / scale
        val parts = rounded.toString().split(".")

        return if (parts.size == 2) {
            val decimals = parts[1].padEnd(precision, '0')
            "${parts[0]}.$decimals"
        }
        else {
            "${parts[0]}." + "0".repeat(precision)
        }
    }


    fun Number.addSuffix(): String {
        val long = this.toLong()
        if (long in 11 .. 13) return "${this}th"
        return when (long % 10) {
            1L -> "${this}st"
            2L -> "${this}nd"
            3L -> "${this}rd"
            else -> "${this}th"
        }
    }

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

    fun Int.toRoman(): String {
        if (this <= 0) error("$this must be positive!")
        val l = romanSymbols.floorKey(this)
        return if (this == l) romanSymbols[this] !!
        else romanSymbols[l] + (this - l).toRoman()
    }

    operator fun Number.div(number: Number): Number =
        this.toDouble() / number.toDouble()

    operator fun Number.times(number: Number): Number =
        this.toDouble() * number.toDouble()

    operator fun Number.minus(number: Number): Number =
        this.toDouble() - number.toDouble()

    operator fun Number.plus(number: Number): Number =
        this.toDouble() + number.toDouble()


    private fun processDecimal(decimal: Int, lastNumber: Int, lastDecimal: Int): Int {
        return if (lastNumber > decimal) lastDecimal - decimal
        else lastDecimal + decimal
    }
}