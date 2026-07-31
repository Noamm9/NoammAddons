package com.github.noamm9.init.types

import com.github.noamm9.commands.CommandBuilder

interface ICommandProvider {
    fun CommandBuilder.command()
}