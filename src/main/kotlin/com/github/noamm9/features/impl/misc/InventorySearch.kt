package com.github.noamm9.features.impl.misc

import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.ui.hud.HudElement
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.ui.utils.TextInputHandler
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.NumbersUtils
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.highlight
import gg.essential.universal.UKeyboard
import gg.essential.universal.UMinecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.MouseButtonInfo
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW
import java.awt.Color

object InventorySearch: Feature("Lets you search in inventory and support math") {
    private val ignoreCaps by ToggleSetting("Ignore Caps", true)
    private val searchLore by ToggleSetting("Search Lore", true)
    private val highlightColor by ColorSetting("Highlight Color", Color.RED)

    private var searchQuery = ""
    private val searchHandler = TextInputHandler({ searchQuery }) {
        expressionResult = evaluateExpression(it)
        searchQuery = it
    }

    private var expressionResult: Double? = null

    val color get() = highlightColor.value
    val isSearching get() = enabled && searchQuery.isNotBlank()

    fun matches(stack: ItemStack): Boolean {
        if (searchQuery.isBlank() || stack.isEmpty) return false
        if (stack.hoverName.unformattedText.contains(searchQuery, ignoreCaps.value)) return true
        return searchLore.value && stack.lore.any { it.removeFormatting().contains(searchQuery, ignoreCaps.value) }
    }

    private lateinit var searchHud: HudElement
    private const val WIDTH = 200f
    private const val HEIGHT = 22f

    override fun init() {
        searchHud = hudElement(
            name = "Inventory Search",
            shouldDraw = { false },
            centered = true
        ) { context, example ->
            searchHandler.x = - WIDTH / 2
            searchHandler.y = 0f
            searchHandler.width = WIDTH
            searchHandler.height = HEIGHT

            val localMouseX = (Resolution.getMouseX() - searchHud.x) / searchHud.scale
            val localMouseY = (Resolution.getMouseY() - searchHud.y) / searchHud.scale

            context.drawRect(- WIDTH / 2, 0f, WIDTH, HEIGHT, Color(15, 15, 15, 200))
            val color = if (searchHandler.listening) Style.accentColor else Color(255, 255, 255, 30)
            context.drawRect(- WIDTH / 2, HEIGHT - 1, WIDTH, 1f, color)

            if (example || searchQuery.isEmpty() && ! searchHandler.listening) context.drawCenteredString("§8Search...", 0f, 6f)
            else if (expressionResult != null) searchHandler.draw(context, localMouseX, localMouseY, " = §e${NumbersUtils.formatComma(expressionResult)}")
            else searchHandler.draw(context, localMouseX, localMouseY)

            WIDTH to HEIGHT
        } defaults {
            x = Resolution.width / 2f
            y = Resolution.height - 30f - HEIGHT / 2f
        }

        register<ScreenEvent.PostRender> {
            if (UMinecraft.currentScreenObj !is AbstractContainerScreen<*>) return@register

            Resolution.push(event.context)
            searchHud.renderElement(event.context, false)
            Resolution.pop(event.context)
        }

        register<MouseClickEvent> {
            if (UMinecraft.currentScreenObj !is AbstractContainerScreen<*>) return@register
            if (event.action == GLFW.GLFW_RELEASE) searchHandler.mouseReleased()
            if (event.action != GLFW.GLFW_PRESS) return@register

            val x = (Resolution.getMouseX() - searchHud.x) / searchHud.scale
            val y = (Resolution.getMouseY() - searchHud.y) / searchHud.scale
            val mbe = MouseButtonEvent(0.0, 0.0, MouseButtonInfo(event.button, event.action))

            if (searchHandler.mouseClicked(x, y, mbe)) event.isCanceled = true
        }

        register<KeyboardEvent.CharTyped> {
            if (UMinecraft.currentScreenObj !is AbstractContainerScreen<*>) return@register
            if (! searchHandler.listening) return@register
            searchHandler.keyTyped(event.charEvent)
            event.isCanceled = true
        }

        register<KeyboardEvent.KeyPressed> {
            if (UMinecraft.currentScreenObj !is AbstractContainerScreen<*>) return@register

            if (event.keyEvent.key == UKeyboard.KEY_F && event.keyEvent.hasControlDown()) {
                searchHandler.listening = ! searchHandler.listening
                event.isCanceled = true
                return@register
            }

            if (! searchHandler.listening) return@register
            searchHandler.keyPressed(event.keyEvent)
            event.isCanceled = true
        }

        register<ContainerEvent.Render.Slot.Pre> {
            if (! matches(event.slot.item)) return@register
            event.slot.highlight(event.context, highlightColor.value, 3)
        }
    }

    // Shunting Yard Algorithm
    private fun evaluateExpression(expr: String): Double? {
        if (expr.isBlank()) return null
        if (expr.none { it.isDigit() }) return null

        val operators = mapOf(
            "+" to (1 to false),
            "-" to (1 to false),
            "*" to (2 to false),
            "x" to (2 to false),
            "/" to (2 to false),
            "u-" to (3 to true) // unary minus
        )

        val tokens = mutableListOf<String>()
        var i = 0

        while (i < expr.length) when {
            expr[i].isWhitespace() -> i ++

            (expr[i] == '+' || expr[i] == '-') &&
                (tokens.isEmpty() || tokens.last() == "(" || tokens.last() in operators) -> {
                if (expr[i] == '-') tokens.add("u-")
                i ++
            }

            expr[i].isDigit() || expr[i] == '.' -> {
                val start = i
                while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i ++
                if (i < expr.length && expr[i].lowercaseChar() in "kmbt") i ++
                tokens.add(expr.substring(start, i))
            }

            expr[i] in "+-*x/()" -> {
                tokens.add(expr[i].toString())
                i ++
            }

            else -> return null
        }

        val output = mutableListOf<String>()
        val stack = ArrayDeque<String>()

        for (token in tokens) {
            val numCheck = NumbersUtils.parseCompactNumberDouble(token)
            when {
                numCheck != null -> output.add(token)

                token == "(" -> stack.addFirst(token)

                token == ")" -> {
                    while (stack.isNotEmpty() && stack.first() != "(") output.add(stack.removeFirst())
                    if (stack.isEmpty() || stack.removeFirst() != "(") return null
                }

                token in operators -> {
                    val (currentPrec, currentRightAssoc) = operators[token] !!
                    while (stack.isNotEmpty() && stack.first() in operators) {
                        val (topPrec, _) = operators[stack.first()] !!
                        val shouldPop = topPrec > currentPrec || (topPrec == currentPrec && ! currentRightAssoc)
                        if (! shouldPop) break
                        output.add(stack.removeFirst())
                    }
                    stack.addFirst(token)
                }

                else -> return null
            }
        }

        while (stack.isNotEmpty()) {
            if (stack.first() == "(" || stack.first() == ")") return null
            output.add(stack.removeFirst())
        }

        val evalStack = ArrayDeque<Double>()

        for (token in output) {
            val num = NumbersUtils.parseCompactNumberDouble(token)

            when {
                num != null -> evalStack.addFirst(num)

                token == "u-" -> {
                    if (evalStack.isEmpty()) return null
                    evalStack.addFirst(- evalStack.removeFirst())
                }

                token in operators -> {
                    if (evalStack.size < 2) return null

                    val b = evalStack.removeFirst()
                    val a = evalStack.removeFirst()

                    val res = when (token) {
                        "+" -> a + b
                        "-" -> a - b
                        "*", "x" -> a * b
                        "/" -> if (b == 0.0) return null else a / b
                        else -> return null
                    }
                    evalStack.addFirst(res)
                }

                else -> return null
            }
        }

        val result = evalStack.singleOrNull() ?: return null
        return result.takeIf { it.isFinite() }
    }
}