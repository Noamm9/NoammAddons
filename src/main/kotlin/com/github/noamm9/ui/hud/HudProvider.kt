package com.github.noamm9.ui.hud

interface HudProvider {
    val hudElements: MutableSet<HudElement>

    infix fun HudElement.defaults(block: HudElement.() -> Unit) = apply { ::defaults.set(block) }
}