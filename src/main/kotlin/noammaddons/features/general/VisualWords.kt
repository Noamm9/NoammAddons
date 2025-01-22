package noammaddons.features.general

import gg.essential.vigilance.gui.SettingsGui
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.JsonUtils.fetchJsonWithRetry
import noammaddons.utils.Utils.isNull

/**
 * @see noammaddons.mixins.MixinFontRenderer
 */
object VisualWords {
    private var wordsMap: Map<String, String>? = null

    init {
        fetchJsonWithRetry<Map<String, String>>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/VisualWords.json"
        ) { wordsMap = it }
    }

    @JvmStatic
    fun replaceText(text: String?): String? {
        if (text.isNull()) return text
        if (wordsMap.isNull()) return text
        if (mc.currentScreen is SettingsGui) return text
        if (config.disableVisualWords) return text

        var newText = text
        for (actualText in wordsMap !!.keys) {
            newText = newText?.replace(actualText, wordsMap !![actualText] !!)
        }

        return newText
    }
}