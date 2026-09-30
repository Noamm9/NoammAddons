plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.3-cheat"

stonecutter parameters {
    val dyeColors = mapOf(
        "black" to "BLACK",
        "blue" to "BLUE",
        "brown" to "BROWN",
        "cyan" to "CYAN",
        "gray" to "GRAY",
        "green" to "GREEN",
        "lightBlue" to "LIGHT_BLUE",
        "lightGray" to "LIGHT_GRAY",
        "lime" to "LIME",
        "magenta" to "MAGENTA",
        "orange" to "ORANGE",
        "pink" to "PINK",
        "purple" to "PURPLE",
        "red" to "RED",
        "white" to "WHITE",
        "yellow" to "YELLOW",
    )

    replacements {
        val (version, varient) = current.project.split("-")
        swaps["mod_version"] = "\"${property("mod_version")}\""
        swaps["mod_id"] = "\"${property("mod_id")}\""
        swaps["mod_name"] = "\"${property("mod_name")}\""
        swaps["mc_version"] = "\"$version\""
        swaps["mod_varient"] = "\"$varient\""

        string(current.parsed < "26.2") {
            replace("EntityTypes.", "EntityType.")
            replace("mc.levelExtractor", "mc.levelRenderer")
            replace("mc.gui.hud.tabList", "mc.gui.tabList")
            dyeColors.forEach { (lower, upper) ->
                replace("DYE.$lower()", "${upper}_DYE")
                replace("WOOL.$lower()", "${upper}_WOOL")
                replace("STAINED_GLASS.$lower()", "${upper}_STAINED_GLASS")
                replace("STAINED_GLASS_PANE.$lower()", "${upper}_STAINED_GLASS_PANE")
                replace("DYED_TERRACOTTA.$lower()", "${upper}_TERRACOTTA")
            }
        }

        string(current.parsed < "26.3") {
            replace("net.minecraft.world.entity.monster.Enderman", "net.minecraft.world.entity.monster.EnderMan")
            replace("com.mojang.renderpearl.api.pipeline.RenderPipeline", "com.mojang.blaze3d.pipeline.RenderPipeline")
            replace("entity is Enderman", "entity is EnderMan")
        }
    }
}