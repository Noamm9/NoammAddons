package noammaddons.features.gui.Menus

object CustomMenuManager {
    val WhiteListMenus = mutableSetOf<Pair<String?, Regex?>>()

    fun addMenu(guiName: String? = null, regex: Regex? = null) {
        WhiteListMenus.add(guiName to regex)
    }

    init {
        listOf(
            "SkyBlock Menu",
            "Game Menu",
            "Storage",

            "Catacombs Gate",
            "Group Builder",
            "Dungeon Classes",
            "Search Settings",
            "Dungeon Rules and Tips",
            "Catacombs Profile",
            "Ready Up",
            "Select Type",
        ).forEach(this::addMenu)
    }
}