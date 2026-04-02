package com.github.noamm9

import com.mojang.blaze3d.pipeline.RenderPipeline
import net.fabricmc.loader.api.FabricLoader
import net.irisshaders.iris.api.v0.IrisApi
import net.irisshaders.iris.api.v0.IrisProgram

enum class IrisShaderType {
    LINES, BASIC
}

/**
 * Created by @j10a1n15
 */
interface IrisCompat {
    fun registerPipeline(pipeline: RenderPipeline, shaderType: IrisShaderType) {}

    companion object : IrisCompat by resolve()
}

internal object IrisCompatImpl : IrisCompat {
    private val instance by lazy { IrisApi.getInstance() }

    override fun registerPipeline(pipeline: RenderPipeline, shaderType: IrisShaderType) {
        val type = when (shaderType) {
            IrisShaderType.BASIC -> IrisProgram.BASIC
            IrisShaderType.LINES -> IrisProgram.LINES
        }
        instance.assignPipeline(pipeline, type)
    }
}

internal object IrisCompatNoOp : IrisCompat

internal fun resolve(): IrisCompat =
    if (FabricLoader.getInstance().isModLoaded("iris")) IrisCompatImpl else IrisCompatNoOp
