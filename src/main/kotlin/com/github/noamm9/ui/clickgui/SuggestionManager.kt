package com.github.noamm9.ui.clickgui

import com.github.noamm9.features.Feature

object SuggestionManager {
    fun getSuggestions(query: String, features: List<Feature>): Collection<Feature> {
        if (query.isBlank()) return features
        return features.mapNotNull { feature ->
            calculateRank(feature, query.normalize())?.let { rank -> feature to rank }
        }.sortedWith(compareBy({ (_, rank) -> rank }, { (feature, _) -> feature.name })).map { (feature, _) ->
            feature
        }.toMutableSet()
    }

    private fun calculateRank(feature: Feature, cleanQuery: String): Int? {
        val featureName = feature.name.normalize()

        if (featureName.startsWith(cleanQuery)) return 0
        if (featureName.contains(cleanQuery)) return 1

        var settingContainsMatch = false
        val visibleSettings = feature.configSettings.filter { it.visibility() }

        for (setting in visibleSettings) {
            val settingName = setting.name.normalize()
            if (settingName.startsWith(cleanQuery)) return 2
            if (settingName.contains(cleanQuery)) settingContainsMatch = true
        }

        if (settingContainsMatch) return 3
        if (feature.description?.normalize()?.contains(cleanQuery) == true) return 5

        return null
    }

    private fun String.normalize() = filterNot(Char::isWhitespace).lowercase()
}