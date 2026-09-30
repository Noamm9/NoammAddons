package com.github.noamm9.utils.render.world.batches

//? if <26.2 {
import org.joml.Matrix4f

class TextRenderState(
    val matrix: Matrix4f,
    val text: String,
    val xOff: Float,
    val yOff: Float,
    val argb: Int,
    val seeThrough: Boolean
)
//? }