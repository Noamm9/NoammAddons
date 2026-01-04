package noammaddons.features.impl.general

import gg.essential.vigilance.gui.SettingsGui
import noammaddons.features.Feature
import noammaddons.ui.config.core.annotations.Dev
import noammaddons.utils.DataDownloader


/**
 * @see noammaddons.mixins.MixinFontRenderer
 */
@Dev
object VisualWords: Feature("Replace some text with other text", toggled = true) {
    private var wordsMap = DataDownloader.loadJson<Map<String, String>>("VisualWords.json")

    @JvmStatic
    fun replaceText(text: String?): String? {
        if (! enabled) return text
        if (text == null) return text
        if (mc.currentScreen is SettingsGui) return text

        return wordsMap.entries.fold(text) { it, entry ->
            it.replace(entry.key, entry.value)
        }
    }
}