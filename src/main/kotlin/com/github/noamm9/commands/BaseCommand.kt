package com.github.noamm9.commands

abstract class BaseCommand(val name: String, val aliases: MutableSet<String> = mutableSetOf()) {
    abstract fun CommandNodeBuilder.build()
}