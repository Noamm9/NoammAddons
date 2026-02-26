package com.github.noamm9.features.impl.misc

import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.event.impl.KeyboardEvent
import com.github.noamm9.event.impl.MouseClickEvent
import com.github.noamm9.event.impl.ScreenEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.Style
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.ui.utils.TextInputHandler
import com.github.noamm9.utils.NumbersUtils
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.highlight
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.MouseButtonInfo
import org.lwjgl.glfw.GLFW
import java.awt.Color

object InventorySearch : Feature("Lets you search in inventory") {
    private var searchQuery: String = ""
    private val searchHandler = TextInputHandler({ searchQuery }, { searchQuery = it })

    private var expressionResult: Double? = null
    const val WIDTH = 200f
    const val HEIGHT = 30f
    private var inContainer: Boolean = false

    override fun init() {
        register<ScreenEvent.PostRender> {
            if (inContainer) {
                Resolution.refresh()
                Resolution.push(event.context)
                val x = (Resolution.width / 2) - (WIDTH / 2)
                val y = (Resolution.height - 40) - (HEIGHT / 2)
                searchHandler.x = x
                searchHandler.y = y
                searchHandler.width = WIDTH
                searchHandler.height = HEIGHT
                val mx = Resolution.getMouseX()
                val my = Resolution.getMouseY()
                Render2D.drawRect(event.context, x, y, WIDTH, HEIGHT, Color(15, 15, 15, 200))
                val color = if (searchHandler.listening) Style.accentColor else Color(255, 255, 255, 30)
                Render2D.drawRect(event.context, x, y + HEIGHT - 1, WIDTH, 1f, color)
                if (searchQuery.isEmpty() && !searchHandler.listening) {
                    Render2D.drawCenteredString(event.context, "§8Search...", x + WIDTH / 2, y + 6)
                }
                else if (expressionResult != null)
                    searchHandler.draw(event.context, mx.toFloat(), my.toFloat(), " §a${expressionResult.toString()}")
                else
                    searchHandler.draw(event.context, mx.toFloat(), my.toFloat())
                Resolution.pop(event.context)

            }
        }

        register<MouseClickEvent> {
            //if (!searchHandler.listening) return@register
            if (event.action == GLFW.GLFW_RELEASE) searchHandler.mouseReleased()
            if (event.action == GLFW.GLFW_PRESS) {
                searchHandler.mouseClicked(
                    Resolution.getMouseX().toFloat(),
                    Resolution.getMouseY().toFloat(),
                    MouseButtonEvent(0.0, 0.0, MouseButtonInfo(event.button, event.action)))
            }
        }

        register<KeyboardEvent.CharTyped> {
            if (!searchHandler.listening) return@register

            searchHandler.keyTyped(event.charEvent)

            val appearance = searchQuery.indexOfFirst { it == '=' }
            expressionResult = if (appearance != -1)
                evaluateExpression(searchQuery.substring(0, appearance))
            else
                null
        }

        register<KeyboardEvent.KeyPressed> {
            if (!searchHandler.listening) return@register

            if (mc.options.keyInventory.matches(event.keyEvent))
                event.isCanceled = true

            searchHandler.keyPressed(event.keyEvent)
        }

        register<ContainerEvent.Render.Slot.Pre> {
            if (searchQuery.isEmpty()) return@register

            if (event.slot.item.itemName.string != "Air" && event.slot.item.itemName.string.contains(searchQuery))
                event.slot.highlight(event.context, Color.RED)
        }

        register<ContainerEvent.Close> {
            inContainer = false
            searchHandler.listening = false
            searchQuery = ""
        }

        register<ContainerEvent.Open> {
            inContainer = true
        }
    }

    //Shunting Yard Algorithm
    //This shit is less stable than I am
    fun evaluateExpression(expr: String): Double? {
        val operators = mapOf(
            "+" to 1,
            "-" to 1,
            "*" to 2,
            "/" to 2
        )

        val tokens = mutableListOf<String>()
        var i = 0

        while (i < expr.length) {
            when {
                expr[i].isDigit() || expr[i] == '.' -> {
                    val start = i
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.'))
                        i++
                    if (i < expr.length && expr[i].lowercaseChar() in "kmbt")
                        i++

                    tokens.add(expr.substring(start, i))
                }

                expr[i] in "+-*/()" -> {
                    tokens.add(expr[i].toString())
                    i++
                }

                expr[i].isWhitespace() -> i++

                else -> return null
            }
        }

        val output = mutableListOf<String>()
        val stack = ArrayDeque<String>()

        for (token in tokens) {
            val numCheck = NumbersUtils.parseCompactNumber(token)?.toDouble()
            when {
                numCheck != null -> output.add(token)

                token == "(" -> stack.addFirst(token)

                token == ")" -> {
                    while (stack.isNotEmpty() && stack.first() != "(") output.add(stack.removeFirst())
                    if (stack.isEmpty() || stack.removeFirst() != "(") return null
                }

                token in operators -> {
                    while (stack.isNotEmpty() && stack.first() in operators && operators[token]!! <= operators[stack.first()]!!) {
                        output.add(stack.removeFirst())
                    }
                    stack.addFirst(token)
                }

                else -> return null
            }
        }

        while (stack.isNotEmpty()) {
            if (stack.first() in listOf("(", ")")) return null
            output.add(stack.removeFirst())
        }

        val evalStack = ArrayDeque<Double>()

        for (token in output) {
            val num = NumbersUtils.parseCompactNumber(token)?.toDouble()

            if (num != null) {
                evalStack.addFirst(num)
            } else if (token in operators) {
                if (evalStack.size < 2) return null

                val b = evalStack.removeFirst()
                val a = evalStack.removeFirst()

                val res = when (token) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> a / b
                    else -> return null
                }
                evalStack.addFirst(res)

            } else return null
        }

        return if (evalStack.size == 1) evalStack.first() else null
    }
}