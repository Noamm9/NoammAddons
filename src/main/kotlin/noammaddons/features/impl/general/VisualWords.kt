package noammaddons.features.impl.general

import gg.essential.vigilance.gui.SettingsGui
import noammaddons.features.Feature
import noammaddons.ui.config.core.annotations.Dev
import noammaddons.utils.WebUtils


/**
 * @see noammaddons.mixins.MixinFontRenderer
 */
@Dev
object VisualWords: Feature("Replace some text with other text", toggled = true) {
    private var wordsMap: Map<String, String>? = null

    init {
        WebUtils.fetchJson<Map<String, String>>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/VisualWords.json"
        ) { wordsMap = it }
    }

    @JvmStatic
    @Suppress("KotlinConstantConditions")
    fun replaceText(text: String?): String? {
        if (! enabled) return text
        if (text == null) return text
        if (wordsMap == null) return text
        if (mc.currentScreen is SettingsGui) return text

        var newText = text
        wordsMap !!.entries.forEach { (key, value) ->
            newText = newText?.replace(key, value)
        }

        return newText
    }
}