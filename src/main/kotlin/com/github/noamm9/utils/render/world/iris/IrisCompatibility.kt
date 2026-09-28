package com.github.noamm9.utils.render.world.iris

import com.github.noamm9.init.ModCompatibility
import com.mojang.blaze3d.pipeline.RenderPipeline
import net.irisshaders.iris.api.v0.IrisApi
import net.irisshaders.iris.api.v0.IrisProgram

/**
 * Created by @j10a1n15
 */
object IrisCompatibility {
    private val compat by lazy { if (ModCompatibility.isModLoaded("iris")) IrisCompatImpl else IrisCompatNoOp }

    fun registerPipeline(pipeline: RenderPipeline, shaderType: IrisShaderType) {
        compat.registerPipeline(pipeline, shaderType)
    }

    private interface IrisCompat {
        fun registerPipeline(pipeline: RenderPipeline, shaderType: IrisShaderType) = Unit
    }

    private object IrisCompatImpl: IrisCompat {
        private val irisApi by lazy { IrisApi.getInstance() }

        override fun registerPipeline(pipeline: RenderPipeline, shaderType: IrisShaderType) {
            irisApi.assignPipeline(pipeline, shaderType.toIrisProgram())
        }

        private fun IrisShaderType.toIrisProgram() = when (this) {
            IrisShaderType.LINES -> IrisProgram.LINES
            IrisShaderType.BASIC -> IrisProgram.BASIC
        }
    }

    private object IrisCompatNoOp: IrisCompat
}