package noammaddons.features.general

import gg.essential.vigilance.gui.SettingsGui
import noammaddons.features.Feature
import noammaddons.utils.JsonUtils.fetchJsonWithRetry

/**
 * @see noammaddons.mixins.MixinFontRenderer
 */
object VisualWords: Feature() {
    private var wordsMap: Map<String, String>? = null

    init {
        fetchJsonWithRetry<Map<String, String>>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/VisualWords.json"
        ) {
            wordsMap = it ?: return@fetchJsonWithRetry
        }
    }

    @JvmStatic
    @Suppress("KotlinConstantConditions")
    fun replaceText(text: String?): String? {
        if (text == null) return text
        if (wordsMap == null) return text
        if (mc.currentScreen is SettingsGui) return text
        if (config.disableVisualWords) return text

        var newText = text
        wordsMap !!.entries.forEach { (key, value) ->
            newText = newText?.replace(key, value)
        }

        return newText
    }
}