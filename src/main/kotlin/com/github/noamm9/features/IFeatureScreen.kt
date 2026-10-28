package com.github.noamm9.features

import net.minecraft.client.gui.screens.Screen

interface IFeatureScreen {
    fun createScreen(): Screen
}