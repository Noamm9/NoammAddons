package com.github.noamm9.config

object PersonalBest {
    private val pbData by PogObject("personal_bests", mutableMapOf<String, Number>())

    /**
     * @return true if this was a new Personal Best, false otherwise.
     */
    fun checkAndSetPB(key: String, value: Number, lowerIsBetter: Boolean = true): Boolean {
        val currentPB = pbData[key]

        val isNewPB = when {
            currentPB == null -> true

            lowerIsBetter && value.toDouble() < currentPB.toDouble() -> true
            ! lowerIsBetter && value.toDouble() > currentPB.toDouble() -> true

            else -> false
        }

        return isNewPB.also { if (it) pbData[key] = value }
    }

    fun getPB(key: String) = pbData[key]
}