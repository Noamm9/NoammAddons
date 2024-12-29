package noammaddons.features.gui.Menus

import noammaddons.noammaddons.Companion.config

object CustomMenuManager {
    val menuList = mutableSetOf<Triple<String?, Regex?, () -> Boolean>>()

    init {
        listOf(
            Triple("Game Menu", null) { config.CustomMenusGameMenu },
            Triple("SkyBlock Menu", null) { config.CustomMenusSkyBlockMenu },
            Triple(null, Regex("^Storage§r$")) { config.CustomMenusStorageMenu },

            Triple("Catacombs Gate", null) { config.CustomPartyFinderMenu },
            Triple("Group Builder", null) { config.CustomPartyFinderMenu },
            Triple("Dungeon Classes", null) { config.CustomPartyFinderMenu },
            Triple("Search Settings", null) { config.CustomPartyFinderMenu },
            Triple("Dungeon Rules and Tips", null) { config.CustomPartyFinderMenu },
            Triple("Catacombs Profile", null) { config.CustomPartyFinderMenu },
            Triple("Ready Up", null) { config.CustomPartyFinderMenu },
            Triple("Select Type", null) { config.CustomPartyFinderMenu },
            Triple("Select Floor", null) { config.CustomPartyFinderMenu },
        ).forEach(menuList::add)
    }
}