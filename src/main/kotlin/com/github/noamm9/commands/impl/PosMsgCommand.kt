package com.github.noamm9.commands.impl

import com.github.noamm9.commands.BaseCommand
import com.github.noamm9.commands.CommandNodeBuilder
import com.github.noamm9.features.impl.dungeon.PositionalMessages
import com.github.noamm9.utils.ChatUtils
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.core.BlockPos
import java.awt.Color

object PosMsgCommand: BaseCommand("posmsg") {
    private val colorMap = mapOf(
        "red" to Color.RED,
        "green" to Color.GREEN,
        "blue" to Color.BLUE,
        "yellow" to Color.YELLOW,
        "cyan" to Color.CYAN,
        "magenta" to Color.MAGENTA,
        "white" to Color.WHITE,
        "orange" to Color.ORANGE,
        "pink" to Color.PINK,
        "gray" to Color.GRAY,
        "purple" to Color(128, 0, 128)
    )

    private fun parseColor(input: String): Color {
        colorMap[input.lowercase()]?.let { return it }
        return runCatching {
            val hex = input.removePrefix("#")
            Color(hex.toLong(16).toInt(), hex.length == 8)
        }.getOrDefault(Color.CYAN)
    }

    override fun CommandNodeBuilder.build() {
        literal("add") {
            chainArgs(
                "x" to IntegerArgumentType.integer(),
                "y" to IntegerArgumentType.integer(),
                "z" to IntegerArgumentType.integer(),
                "radius" to DoubleArgumentType.doubleArg(0.1),
                "delay" to DoubleArgumentType.doubleArg(0.0),
                "color" to StringArgumentType.word(),
                "message" to StringArgumentType.greedyString()
            ) {
                runs { ctx ->
                    val x = IntegerArgumentType.getInteger(ctx, "x")
                    val y = IntegerArgumentType.getInteger(ctx, "y")
                    val z = IntegerArgumentType.getInteger(ctx, "z")
                    val radius = DoubleArgumentType.getDouble(ctx, "radius")
                    val delay = DoubleArgumentType.getDouble(ctx, "delay")
                    val colorInput = StringArgumentType.getString(ctx, "color")
                    val message = StringArgumentType.getString(ctx, "message")

                    val color = parseColor(colorInput)
                    PositionalMessages.posMessages.add(PositionalMessages.PosMessage(BlockPos(x, y, z), delay, radius, color, message))
                    PositionalMessages.saveConfig()
                    ChatUtils.modMessage("&aAdded positional message at &e$x, $y, $z &awith delay &e${delay}s &aand color &e$colorInput")
                }
            }
        }

        literal("remove") {
            argument("index", IntegerArgumentType.integer(0)) {
                runs {
                    val index = IntegerArgumentType.getInteger(it, "index")
                    if (index >= PositionalMessages.posMessages.size) {
                        ChatUtils.modMessage("&cIndex out of range.")
                        return@runs
                    }
                    PositionalMessages.posMessages.removeAt(index)
                    PositionalMessages.saveConfig()
                    ChatUtils.modMessage("&aRemoved message at index $index.")
                }
            }
        }

        literal("list") {
            runs {
                if (PositionalMessages.posMessages.isEmpty()) {
                    ChatUtils.modMessage("&cNo positional messages set.")
                    return@runs
                }
                PositionalMessages.posMessages.forEachIndexed { i, msg ->
                    ChatUtils.modMessage("&e$i&f: &7(${msg.pos}) r=${msg.radius} delay=${msg.delay}s &f-> &b${msg.message}")
                }
            }
        }

        literal("clear") {
            runs {
                PositionalMessages.posMessages.clear()
                PositionalMessages.saveConfig()
                ChatUtils.modMessage("&aCleared all positional messages.")
            }
        }

        runs {
            ChatUtils.modMessage("&eUsage: /posmsg add <x> <y> <z> <radius> <delay_seconds> <color> <message>")
            ChatUtils.modMessage("&eColors: red, green, blue, yellow, cyan, magenta, white, orange, pink, gray, purple, or #RRGGBB")
            ChatUtils.modMessage("&eOther: /posmsg remove <index> | list | clear")
        }
    }
}