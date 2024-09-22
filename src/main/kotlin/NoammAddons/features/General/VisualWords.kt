package NoammAddons.features.General

import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.JsonUtils.fetchJsonWithRetry


object VisualWords {
    private var wordsMap: Map<String, String>? = null

    init {
        fetchJsonWithRetry<Map<String, String>>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/data/VisualWords.json"
        ) { wordsMap = it }
    }

    /**
     * @see NoammAddons.mixins.MixinFontRenderer
    */
    fun replaceText(text: String?): String? {
        if (text == null) return text
        if (wordsMap == null) return text
		if (mc.currentScreen?.javaClass?.name == "gg.essential.vigilance.gui.Settingsgui") return text
	    
        var replacedText = text
        for (actualText in wordsMap!!.keys) {
            replacedText = replacedText?.replace(actualText, wordsMap!![actualText]!!)
        }
        return replacedText
    }
}