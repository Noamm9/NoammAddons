package com.github.noamm9.compat

import net.minecraft.client.renderer.rendertype.RenderSetup

enum class OitMode { FILLED, LINES }

fun RenderSetup.RenderSetupBuilder.withOit(mode: OitMode): RenderSetup.RenderSetupBuilder {
    //? if <26.3 {
    /*// 26.2 does not have OIT pipelines.
    return this
    */ //? } else {
    return setOitPipelines(
        when (mode) {
            OitMode.FILLED -> net.minecraft.client.renderer.RenderPipelines.OIT_DEBUG_FILLED_BOX
            OitMode.LINES -> net.minecraft.client.renderer.RenderPipelines.OIT_LINES_TRANSLUCENT
        }
    )
    //? }
}