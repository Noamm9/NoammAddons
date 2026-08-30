package com.github.noamm9.features.impl.visual

import com.github.noamm9.config.types.MultiCheckboxSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.ActionBarMessageEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ActionBarParser
import com.github.noamm9.utils.NumbersUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.height
import com.github.noamm9.utils.render.RenderHelper.width

object PlayerHud: Feature(name = "Player HUD", description = "Displays your stats as moveable HUD elements.") {
    private val elements by MultiCheckboxSetting("Elements", mutableMapOf(
        "Health" to false,
        "Defense" to false,
        "Mana" to false,
        "Overflow Mana" to false,
        "Vitality" to false,
        "Effective HP" to false,
        "Speed" to false
    ))

    private val hideFromActionbar by MultiCheckboxSetting("Hide from Actionbar", mutableMapOf(
        "Health" to false,
        "Defense" to false,
        "Mana" to false,
        "Overflow Mana" to false,
        "Vitality" to false,
        "Dungeon Room Secrets" to false,
        "Armor Stacks" to false,
        "Terminator Stacks" to false
    ))

    @JvmStatic val hideFoodbar by ToggleSetting("Hide Food bar").withDescription("Hides the food bar.").section("Extras")
    @JvmStatic val hideHealthbar by ToggleSetting("Hide Health bar").withDescription("Hides the health bar.")
    @JvmStatic val hideArmorbar by ToggleSetting("Hide Armor bar").withDescription("Hides the defense bar.").hideIf { hideHealthbar.value }

    override fun init() {
        hudElement(
            this.name + " Health",
            { elements.value["Health"] == true },
            { LocationUtils.inSkyblock }
        ) { context, example ->
            val text = if (example) "§e3452§f/§c2452" else getHpFormatted()
            context.drawString(text, 0, 0)
            return@hudElement text.width() to text.height()
        }

        hudElement(
            this.name + " Defense",
            { elements.value["Defense"] == true },
            { LocationUtils.inSkyblock }
        ) { context, example ->
            val text = if (example) "§a5001" else "§a${ActionBarParser.currentDefense}"
            context.drawString(text, 0, 0)
            return@hudElement text.width() to text.height()
        }

        hudElement(
            this.name + " Mana",
            { elements.value["Mana"] == true },
            { LocationUtils.inSkyblock }
        ) { context, example ->
            val text = if (example) "§b2452/2452" else "§b${ActionBarParser.currentMana}/${ActionBarParser.maxMana}"
            context.drawString(text, 0, 0)
            return@hudElement text.width() to text.height()
        }

        hudElement(
            this.name + " Overflow Mana",
            { elements.value["Overflow Mana"] == true },
            { LocationUtils.inSkyblock && ActionBarParser.overflowMana > 0 }
        ) { context, example ->
            val text = if (example) "§3600ʬ" else "§3${ActionBarParser.overflowMana}ʬ"
            context.drawString(text, 0, 0)
            return@hudElement text.width() to text.height()
        }

        hudElement(
            this.name + " Vitality",
            { elements.value["Vitality"] == true },
            { LocationUtils.inSkyblock && ActionBarParser.isVitalityShown }
        ) { context, example ->
            val text = if (example) "§482/122" else "§4${ActionBarParser.currentVitality}/${ActionBarParser.maxVitality}"
            context.drawString("$text&l♨", 0, 0)
            return@hudElement text.width() to text.height()
        }

        hudElement(
            this.name + " Effective HP",
            { elements.value["Effective HP"] == true },
            { LocationUtils.inSkyblock }
        ) { context, example ->
            val text = if (example) "§27.3m" else "§2${NumbersUtils.format(ActionBarParser.effectiveHP)}"
            context.drawString(text, 0, 0)
            return@hudElement text.width() to text.height()
        }

        hudElement(
            this.name + " Speed",
            { elements.value["Speed"] == true },
            { LocationUtils.inSkyblock }
        ) { context, example ->
            val text = if (example) "§f400✦" else "§f${ActionBarParser.currentSpeed}✦"
            context.drawString(text, 0, 0)
            return@hudElement text.width() to text.height()
        }

        register<ActionBarMessageEvent> {
            if (! LocationUtils.inSkyblock) return@register
            var result = event.formattedText

            if (hideFromActionbar.value["Health"] == true) result = result.replace(ActionBarParser.HP_REGEX, "")
            if (hideFromActionbar.value["Defense"] == true) result = result.replace(ActionBarParser.DEF_REGEX, "")
            if (hideFromActionbar.value["Mana"] == true) result = result.replace(ActionBarParser.MANA_REGEX, "")
            if (hideFromActionbar.value["Overflow Mana"] == true) result = result.replace(ActionBarParser.OVERFLOW_REGEX, "")
            if (hideFromActionbar.value["Vitality"] == true) result = result.replace(ActionBarParser.VITALITY_REGEX, "")
            if (hideFromActionbar.value["Dungeon Room Secrets"] == true) result = result.replace(ActionBarParser.SECRETS_REGEX, "")
            if (hideFromActionbar.value["Armor Stacks"] == true) result = result.replace(ActionBarParser.STACKS_REGEX, "")
            if (hideFromActionbar.value["Terminator Stacks"] == true) result = result.replace(ActionBarParser.SALVATION_REGEX, "")

            event.message = result
        }
    }

    private fun getHpFormatted(): String {
        val color = if (ActionBarParser.currentHealth > ActionBarParser.maxHealth) "§e" else "§c"
        return "$color${ActionBarParser.currentHealth}§f/§c${ActionBarParser.maxHealth}"
    }
}