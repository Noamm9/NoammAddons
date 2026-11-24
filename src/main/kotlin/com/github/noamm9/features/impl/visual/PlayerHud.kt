package com.github.noamm9.features.impl.visual

import com.github.noamm9.config.types.MultiCheckboxSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.ActionBarMessageEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.*
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.height
import com.github.noamm9.utils.render.RenderHelper.width

object PlayerHud: Feature(name = "Player HUD", description = "Displays your stats as moveable HUD elements.") {
    private val elements by MultiCheckboxSetting(
        "Elements", mutableMapOf(
            "Health" to false,
            "Defense" to false,
            "Mana" to false,
            "Overflow Mana" to false,
            "Vitality" to false,
            "Effective HP" to false,
            "Speed" to false
        )
    )

    private val hideFromActionbar by MultiCheckboxSetting(
        "Hide from Actionbar", mutableMapOf(
            "Health" to false,
            "Defense" to false,
            "True Defense" to false,
            "Mana" to false,
            "Overflow Mana" to false,
            "Vitality" to false,
            "Dungeon Room Secrets" to false,
            "Armor Stacks" to false,
            "Terminator Stacks" to false,
            "Skill XP" to false,
            "Essence Drops" to false
        )
    )

    @JvmStatic val hideFoodbar by ToggleSetting("Hide Food bar").withDescription("Hides the food bar.").section("Extras")
    @JvmStatic val hideHealthbar by ToggleSetting("Hide Health bar").withDescription("Hides the health bar.")
    @JvmStatic val hideArmorbar by ToggleSetting("Hide Armor bar").withDescription("Hides the defense bar.").hideIf { hideHealthbar.value }

    override fun init() {
        hudElement(
            this.name + " Health",
            { elements["Health"] },
            { LocationUtils.inSkyblock }
        ) { context, example ->
            val text = if (example) "§e3452§f/§c2452" else getHpFormatted()
            context.drawString(text, 0, 0)
            return@hudElement text.width() to text.height()
        }

        hudElement(
            this.name + " Defense",
            { elements["Defense"] },
            { LocationUtils.inSkyblock }
        ) { context, example ->
            val text = if (example) "§a5001" else "§a${ActionBarParser.currentDefense}"
            context.drawString(text, 0, 0)
            return@hudElement text.width() to text.height()
        }

        hudElement(
            this.name + " Mana",
            { elements["Mana"] },
            { LocationUtils.inSkyblock }
        ) { context, example ->
            val text = if (example) "§b2452/2452" else "§b${ActionBarParser.currentMana}/${ActionBarParser.maxMana}"
            context.drawString(text, 0, 0)
            return@hudElement text.width() to text.height()
        }

        hudElement(
            this.name + " Overflow Mana",
            { elements["Overflow Mana"] },
            { LocationUtils.inSkyblock && ActionBarParser.overflowMana > 0 }
        ) { context, example ->
            val text = if (example) "§3600ʬ" else "§3${ActionBarParser.overflowMana}ʬ"
            context.drawString(text, 0, 0)
            return@hudElement text.width() to text.height()
        }

        hudElement(
            this.name + " Vitality",
            { elements["Vitality"] },
            { LocationUtils.inSkyblock && ActionBarParser.isVitalityShown }
        ) { context, example ->
            val text = if (example) "§482/122" else "§4${ActionBarParser.currentVitality}/${ActionBarParser.maxVitality}"
            context.drawString("$text&l♨", 0, 0)
            return@hudElement text.width() to text.height()
        }

        hudElement(
            this.name + " Effective HP",
            { elements["Effective HP"] },
            { LocationUtils.inSkyblock }
        ) { context, example ->
            val text = if (example) "§27.3m" else "§2${NumbersUtils.format(ActionBarParser.effectiveHP)}"
            context.drawString(text, 0, 0)
            return@hudElement text.width() to text.height()
        }

        hudElement(
            this.name + " Speed",
            { elements["Speed"] },
            { LocationUtils.inSkyblock }
        ) { context, example ->
            val text = if (example) "§f400✦" else "§f${ActionBarParser.currentSpeed}✦"
            context.drawString(text, 0, 0)
            return@hudElement text.width() to text.height()
        }

        register<ActionBarMessageEvent> {
            if (! LocationUtils.inSkyblock) return@register
            var result = event.formattedText

            if (hideFromActionbar["Health"]) result = result.remove(ActionBarParser.HP_REGEX)
            if (hideFromActionbar["Defense"]) result = result.remove(ActionBarParser.DEF_REGEX)
            if (hideFromActionbar["True Defense"]) result = result.remove(ActionBarParser.TRUE_DEF_REGEX)
            if (hideFromActionbar["Mana"]) result = result.remove(ActionBarParser.MANA_REGEX)
            if (hideFromActionbar["Overflow Mana"]) result = result.remove(ActionBarParser.OVERFLOW_REGEX)
            if (hideFromActionbar["Vitality"]) result = result.remove(ActionBarParser.VITALITY_REGEX)
            if (hideFromActionbar["Dungeon Room Secrets"]) result = result.remove(ActionBarParser.SECRETS_REGEX)
            if (hideFromActionbar["Armor Stacks"]) result = result.remove(ActionBarParser.STACKS_REGEX)
            if (hideFromActionbar["Terminator Stacks"]) result = result.remove(ActionBarParser.SALVATION_REGEX)
            if (hideFromActionbar["Skill XP"]) result = result.remove(ActionBarParser.SKILL_XP_REGEX)
            if (hideFromActionbar["Essence Drops"]) result = result.remove(ActionBarParser.ESSENCE_REGEX)

            event.message = result
        }
    }

    private fun getHpFormatted(): String {
        val color = if (ActionBarParser.currentHealth > ActionBarParser.maxHealth) "§e" else "§c"
        return "$color${ActionBarParser.currentHealth}§f/§c${ActionBarParser.maxHealth}"
    }
}