package com.github.noamm9.features.impl.misc

import com.github.noamm9.config.types.*
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.init.NetworkLoop
import com.github.noamm9.utils.*
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.WorldType
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.renderBoundingBox
import com.github.noamm9.utils.render.RenderHelper.renderVec
import com.github.noamm9.utils.render.RenderHelper.width
import com.github.noamm9.utils.render.world.Render3D.renderBlock
import com.github.noamm9.utils.render.world.Render3D.renderBoxBounds
import com.github.noamm9.utils.render.world.Render3D.renderString
import com.github.noamm9.utils.render.world.Render3D.renderTracer
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.chicken.Chicken
import net.minecraft.world.entity.animal.cow.Cow
import net.minecraft.world.entity.animal.equine.Horse
import net.minecraft.world.entity.animal.pig.Pig
import net.minecraft.world.entity.animal.rabbit.Rabbit
import net.minecraft.world.entity.animal.sheep.Sheep
import java.awt.Color

/**
 * Based off Nebulune by Starred
 * under BSD-3 Clause license https://github.com/skies-starred/Nebulune/blob/master/LICENSE
 * @link https://github.com/skies-starred/Nebulune/blob/master/src/main/kotlin/foo/starred/nebulune/modules/impl/general/TrevorHelper.kt
 * Modified
 */
object TrevorHelper: Feature("Helper features for Trevor the Trapper on the Farming Islands.") {
    private val hud by ToggleSetting("Cooldown Timer").section("HUD")

    private val mobEsp by ToggleSetting("Animal ESP", true).section("ESP")
    private val espTracer by ToggleSetting("Show tracer", true).showIf { mobEsp.value }

    private val autoAccept by ToggleSetting("Auto accept", false).section("Automation")
    private val autoCall by ToggleSetting("Auto call", false)
    private val callOff by SliderSetting("Call early", 2.0f, 0.0f, 5.0f, 0.1f, "s").showIf { autoCall.value }

    private val colorTrackable by ColorSetting("Trackable color", Color(205, 214, 244), false).section("Colors")
    private val colorUntrackable by ColorSetting("Untrackable color", Color(166, 227, 161), false)
    private val colorUndetected by ColorSetting("Undetected color", Color(137, 180, 250), false)
    private val colorEndangered by ColorSetting("Endangered color", Color(203, 166, 247), false)
    private val colorElusive by ColorSetting("Elusive color", Color(249, 226, 175), false)

    private val startRegex = Regex("\\[NPC] Trevor: You can find your (?<rarity>.*) animal near the (?<location>.*)\\.")

    private var mobRarity: Rarity? = null
    private var mobArea: MobArea? = null
    private var cooldown = 0L

    override fun init() {
        hudElement(
            name = "$name - Cooldown timer",
            enabled = { hud.value },
            shouldDraw = { LocationUtils.world == WorldType.TheBarn && cooldown > 0L }
        ) { ctx, example ->
            val text = if (example) "Cooldown: §c12.4s"
            else {
                val remaining = (cooldown / 20.0).coerceAtLeast(.0)
                "Cooldown: §c${remaining.toFixed(1)}s"
            }

            ctx.drawString(text, 0, 0)
            return@hudElement text.width().toFloat() to 9f
        }

        register<WorldChangeEvent> { reset() }

        register<RenderWorldEvent> {
            if (! mobEsp.value) return@register
            if (LocationUtils.world != WorldType.TheBarn) return@register

            val level = mc.level ?: return@register
            val area = mobArea ?: return@register
            val rarity = mobRarity ?: return@register

            for (entity in level.entitiesForRendering()) {
                val living = entity as? LivingEntity ?: continue
                if (! living.isTrevorAnimal()) continue
                if (living.serverMaxHealth() != rarity.hp) continue

                event.ctx.renderBoxBounds(
                    living.renderBoundingBox,
                    rarity.color.value.withAlpha(85),
                    outline = true,
                    fill = true,
                    phase = true,
                )

                if (espTracer.value) event.ctx.renderTracer(living.renderVec.add(0.0, living.bbHeight / 2.0, 0.0), rarity.color.value)
            }

            if (area.pos.distSqr(player.blockPosition()) < 4000) return@register
            event.ctx.renderBlock(area.pos, rarity.color.value, rarity.color.value, outline = true, fill = true, phase = true)
            event.ctx.renderString(area.location.lowercase().uppercaseFirst(), area.pos.x, area.pos.y + 4, area.pos.z, scale = 9, phase = true, color = rarity.color.value)
        }

        register<ChatMessageEvent> {
            if (LocationUtils.world != WorldType.TheBarn) return@register

            val stripped = event.unformattedText

            startRegex.find(stripped)?.let { match ->
                val type = match.groups["rarity"]?.value ?: return@register
                val location = match.groups["location"]?.value ?: return@register
                mobRarity = Rarity.from(type) ?: return@register
                mobArea = MobArea.from(location) ?: return@register
                cooldown = 400
                return@register
            }

            if (stripped == "Return to the Trapper soon to get a new animal to hunt!") {
                if (autoCall.value) ThreadUtils.scheduledTaskServer(cooldown - (callOff.value * 20)) {
                    ChatUtils.sendCommand("call trevor")
                }
                reset()
            }

            if (! autoAccept.value) return@register
            if (stripped == "\nAccept the trapper's task to hunt the animal?\nClick an option: [YES] - [NO]") {
                val command = event.component.findRunCommand() ?: return@register
                ThreadUtils.scheduledTask(2) { ChatUtils.sendCommand(command.removePrefix("/")) }
            }
        }

        register<TickEvent.Server> { if (cooldown > 0L) cooldown -- }
    }

    private fun reset() {
        mobRarity = null
        mobArea = null
        cooldown = 0L
    }

    private fun LivingEntity.serverMaxHealth(): Float {
        val max = getAttributeBaseValue(Attributes.MAX_HEALTH).toFloat()
        return if (this is Horse) max.div(2) else max
    }

    private fun LivingEntity.isTrevorAnimal() = when (this) {
        is Cow, is Pig, is Sheep, is Chicken, is Rabbit, is Horse -> true
        else -> false
    }

    private fun Component.findRunCommand(): String? {
        (style.clickEvent as? ClickEvent.RunCommand)?.let { return it.command() }
        for (sibling in siblings) sibling.findRunCommand()?.let { return it }
        return null
    }

    private enum class Rarity(private val normal: Float, val color: ColorSetting) {
        Trackable(100f, colorTrackable),
        Untrackable(500f, colorUntrackable),
        Undetected(1000f, colorUndetected),
        Endangered(5000f, colorEndangered),
        Elusive(10000f, colorElusive);

        val hp get() = if (isDerpy) normal * 2 else normal

        companion object {
            private val isDerpy get() = NetworkLoop.electionData.mayor.name.equals("Derpy", ignoreCase = true)
            fun from(type: String): Rarity? = entries.find { it.name.equals(type, ignoreCase = true) }
        }
    }

    // Taken from SkyHanni
    enum class MobArea(val location: String, val pos: BlockPos) {
        OASIS("Oasis", BlockPos(126, 77, - 456)),
        GORGE("Mushroom Gorge", BlockPos(300, 80, - 509)),
        OVERGROWN("Overgrown Mushroom Cave", BlockPos(242, 60, - 389)),
        SETTLEMENT("Desert Settlement", BlockPos(184, 86, - 384)),
        GLOWING("Glowing Mushroom Cave", BlockPos(199, 50, - 512)),
        MOUNTAIN("Desert Mountain", BlockPos(255, 148, - 518)),
        FOUND("    ", BlockPos.ZERO),
        NONE("   ", BlockPos.ZERO);

        companion object {
            fun from(location: String): MobArea? = entries.find { it.location.equals(location, ignoreCase = true) }
        }
    }
}