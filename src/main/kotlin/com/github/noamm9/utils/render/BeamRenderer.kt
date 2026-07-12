package com.github.noamm9.utils.render

import com.github.noamm9.utils.MathUtils
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.world.phys.Vec3
import java.awt.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

object BeamRenderer {
    const val MAX_SIDES = 32
    private const val MAX_RIBBON_VERTS = 2
    private const val MAX_TUBE_SEGMENTS = 96

    private const val SPIRAL_TURNS = 3f
    private const val HELIX_TURNS = 4f
    private const val SPIRAL_ROT_SPEED = 5.5f
    private const val WAVE_FREQ = 3.5f
    private const val WAVE_SPEED = 5f
    private const val TAPER_LENGTH = 0.15f
    private val TWO_PI = (PI * 2.0).toFloat()

    private const val TRAIL_DETAIL = 0.5f
    private const val GLOW_DETAIL = 0.6f
    private const val MIN_SIDES = 4
    private const val MIN_SEGS = 8

    enum class BeamShape { STRAIGHT, CYLINDER, SPIRAL, DOUBLE_HELIX, WAVE, RIBBON, LIGHTNING }
    enum class ColorMode { STATIC, GRADIENT, RAINBOW, CHROMA }

    data class BeamStyle(
        val shape: BeamShape,
        val colorMode: ColorMode,
        val primary: Color,
        val secondary: Color,
        val width: Float,
        val opacity: Float,
        val length: Float,
        val segments: Int,
        val smoothness: Int,
        val glow: Boolean,
        val throughWalls: Boolean,
        val endpointFade: Boolean,
        val pulse: Boolean,
        val trail: Boolean
    )

    private val ringPointsA = FloatArray(MAX_SIDES * 3)
    private val ringPointsB = FloatArray(MAX_SIDES * 3)
    private val ringColorsA = FloatArray(MAX_SIDES * 4)
    private val ringColorsB = FloatArray(MAX_SIDES * 4)

    private val ribbonPointsA = FloatArray(MAX_RIBBON_VERTS * 3)
    private val ribbonPointsB = FloatArray(MAX_RIBBON_VERTS * 3)
    private val ribbonColorsA = FloatArray(MAX_RIBBON_VERTS * 4)
    private val ribbonColorsB = FloatArray(MAX_RIBBON_VERTS * 4)

    private val unitCircleCos = FloatArray(MAX_SIDES)
    private val unitCircleSin = FloatArray(MAX_SIDES)

    private fun fillUnitCircle(sides: Int) {
        for (j in 0 until sides) {
            val angle = (j.toFloat() / sides.toFloat()) * TWO_PI
            unitCircleCos[j] = cos(angle)
            unitCircleSin[j] = sin(angle)
        }
    }

    fun render(
        ctx: RenderContext,
        origin: Vec3,
        direction: Vec3,
        style: BeamStyle,
        animTime: Float,
        growProgress: Float,
        fadeAlpha: Float,
        seed: Int
    ) {
        if (fadeAlpha <= 0.005f || style.opacity <= 0.005f) return
        val visibleLength = style.length * growProgress
        if (visibleLength <= 0.02f) return

        val dir = if (direction.lengthSqr() < 1.0E-6) Vec3(0.0, 0.0, 1.0) else direction.normalize()
        val (right, up) = buildBasis(dir)

        ctx.matrixStack.pushPose()
        ctx.matrixStack.translate(ctx.camera.position().reverse())
        val pose = ctx.matrixStack.last()

        val layer = if (style.throughWalls) NoammRenderLayers.FILLED_THROUGH_WALLS else NoammRenderLayers.FILLED
        val buffer = ctx.consumers.getBuffer(layer)

        val ox = origin.x.toFloat()
        val oy = origin.y.toFloat()
        val oz = origin.z.toFloat()

        if (style.trail) {
            for (i in 3 downTo 1) {
                val trailAnim = animTime - i * 0.05f
                if (trailAnim < 0f) continue
                val trailAlpha = fadeAlpha * (0.30f - i * 0.07f)
                if (trailAlpha <= 0.01f) continue
                drawShape(buffer, pose, ox, oy, oz, dir, right, up, style, trailAnim, visibleLength, trailAlpha, seed, glowPass = false, radiusMult = 1f, detail = TRAIL_DETAIL)
            }
        }

        if (style.glow) {
            drawShape(buffer, pose, ox, oy, oz, dir, right, up, style, animTime, visibleLength, fadeAlpha, seed, glowPass = true, radiusMult = 2.4f, detail = GLOW_DETAIL)
        }

        drawShape(buffer, pose, ox, oy, oz, dir, right, up, style, animTime, visibleLength, fadeAlpha, seed, glowPass = false, radiusMult = 1f, detail = 1f)

        ctx.matrixStack.popPose()
    }

    private fun buildBasis(dir: Vec3): Pair<Vec3, Vec3> {
        val worldUp = if (kotlin.math.abs(dir.y) > 0.98) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)
        val right = dir.cross(worldUp).normalize()
        val up = right.cross(dir).normalize()
        return right to up
    }

    private fun drawShape(
        buffer: VertexConsumer, pose: PoseStack.Pose,
        ox: Float, oy: Float, oz: Float,
        dir: Vec3, right: Vec3, up: Vec3,
        style: BeamStyle, animTime: Float, visibleLength: Float, alphaMult: Float,
        seed: Int, glowPass: Boolean, radiusMult: Float, detail: Float
    ) {
        if (style.shape == BeamShape.RIBBON) {
            drawRibbon(buffer, pose, ox, oy, oz, dir, right, up, style, animTime, visibleLength, alphaMult, seed, glowPass, radiusMult, detail)
        }
        else {
            drawTube(buffer, pose, ox, oy, oz, dir, right, up, style, animTime, visibleLength, alphaMult, seed, glowPass, radiusMult, detail)
        }
    }

    private fun drawTube(
        buffer: VertexConsumer, pose: PoseStack.Pose,
        ox: Float, oy: Float, oz: Float,
        dir: Vec3, right: Vec3, up: Vec3,
        style: BeamStyle, animTime: Float, visibleLength: Float, alphaMult: Float,
        seed: Int, glowPass: Boolean, radiusMult: Float, detail: Float
    ) {
        val strandCount = if (style.shape == BeamShape.DOUBLE_HELIX) 2 else 1
        val fullSides = style.smoothness.coerceIn(3, MAX_SIDES)
        val fullSegs = style.segments.coerceIn(2, MAX_TUBE_SEGMENTS)
        val sides = (fullSides * detail).toInt().coerceIn(MIN_SIDES.coerceAtMost(fullSides), fullSides)
        val segs = (fullSegs * detail).toInt().coerceIn(MIN_SEGS.coerceAtMost(fullSegs), fullSegs)
        val baseRadius = (style.width * 0.5f * radiusMult).coerceAtLeast(0.01f)
        val amplitude = style.width * 3.5f
        val passAlpha = if (glowPass) alphaMult * 0.35f else alphaMult

        val dirX = dir.x.toFloat(); val dirY = dir.y.toFloat(); val dirZ = dir.z.toFloat()
        val rightX = right.x.toFloat(); val rightY = right.y.toFloat(); val rightZ = right.z.toFloat()
        val upX = up.x.toFloat(); val upY = up.y.toFloat(); val upZ = up.z.toFloat()

        fillUnitCircle(sides)

        for (strand in 0 until strandCount) {
            var prevPoints: FloatArray? = null
            var prevColors: FloatArray? = null
            var useA = true

            for (i in 0 .. segs) {
                val t = i.toFloat() / segs.toFloat()
                val (u, v) = shapeOffset(style.shape, t, animTime, amplitude, strand, seed)
                val radius = tubeRadius(style, t, animTime, baseRadius) * (if (glowPass) 1.6f else 1f)

                val cx = ox + dirX * (t * visibleLength) + rightX * u + upX * v
                val cy = oy + dirY * (t * visibleLength) + rightY * u + upY * v
                val cz = oz + dirZ * (t * visibleLength) + rightZ * u + upZ * v

                val color = colorAt(style, t, animTime, passAlpha)
                val r = color.red / 255f; val g = color.green / 255f; val b = color.blue / 255f; val a = color.alpha / 255f

                val points = if (useA) ringPointsA else ringPointsB
                val colors = if (useA) ringColorsA else ringColorsB

                for (j in 0 until sides) {
                    val cosA = unitCircleCos[j]
                    val sinA = unitCircleSin[j]
                    val px = cx + (rightX * cosA + upX * sinA) * radius
                    val py = cy + (rightY * cosA + upY * sinA) * radius
                    val pz = cz + (rightZ * cosA + upZ * sinA) * radius

                    val p = j * 3
                    points[p] = px; points[p + 1] = py; points[p + 2] = pz

                    val c = j * 4
                    colors[c] = r; colors[c + 1] = g; colors[c + 2] = b; colors[c + 3] = a
                }

                if (prevPoints != null && prevColors != null) {
                    for (j in 0 until sides) {
                        val jn = (j + 1) % sides
                        emitRingQuad(buffer, pose, prevPoints, prevColors, points, colors, j, jn)
                    }
                }

                prevPoints = points
                prevColors = colors
                useA = ! useA
            }
        }
    }

    private fun drawRibbon(
        buffer: VertexConsumer, pose: PoseStack.Pose,
        ox: Float, oy: Float, oz: Float,
        dir: Vec3, right: Vec3, up: Vec3,
        style: BeamStyle, animTime: Float, visibleLength: Float, alphaMult: Float,
        seed: Int, glowPass: Boolean, radiusMult: Float, detail: Float
    ) {
        val fullSegs = style.segments.coerceIn(2, MAX_TUBE_SEGMENTS)
        val segs = (fullSegs * detail).toInt().coerceIn(MIN_SEGS.coerceAtMost(fullSegs), fullSegs)
        val halfWidth = (style.width * 1.5f * radiusMult).coerceAtLeast(0.01f)
        val amplitude = style.width * 3.5f
        val passAlpha = if (glowPass) alphaMult * 0.35f else alphaMult
        val pulseMult = if (style.pulse) 1f + 0.18f * sin(animTime * TWO_PI * 1.6f) else 1f

        val dirX = dir.x.toFloat(); val dirY = dir.y.toFloat(); val dirZ = dir.z.toFloat()
        val rightX = right.x.toFloat(); val rightY = right.y.toFloat(); val rightZ = right.z.toFloat()
        val upX = up.x.toFloat(); val upY = up.y.toFloat(); val upZ = up.z.toFloat()

        var prevPoints: FloatArray? = null
        var prevColors: FloatArray? = null
        var useA = true

        for (i in 0 .. segs) {
            val t = i.toFloat() / segs.toFloat()
            val (u, v) = shapeOffset(BeamShape.RIBBON, t, animTime, amplitude, 0, seed)
            val hw = halfWidth * (if (style.endpointFade) (0.3f + 0.7f * taper(t)) else 1f) * pulseMult

            val cx = ox + dirX * (t * visibleLength) + rightX * u + upX * v
            val cy = oy + dirY * (t * visibleLength) + rightY * u + upY * v
            val cz = oz + dirZ * (t * visibleLength) + rightZ * u + upZ * v

            val color = colorAt(style, t, animTime, passAlpha)
            val r = color.red / 255f; val g = color.green / 255f; val b = color.blue / 255f; val a = color.alpha / 255f

            val points = if (useA) ribbonPointsA else ribbonPointsB
            val colors = if (useA) ribbonColorsA else ribbonColorsB

            points[0] = cx - rightX * hw; points[1] = cy - rightY * hw; points[2] = cz - rightZ * hw
            points[3] = cx + rightX * hw; points[4] = cy + rightY * hw; points[5] = cz + rightZ * hw
            for (k in 0 until 2) {
                val c4 = k * 4
                colors[c4] = r; colors[c4 + 1] = g; colors[c4 + 2] = b; colors[c4 + 3] = a
            }

            if (prevPoints != null && prevColors != null) {
                emitRingQuad(buffer, pose, prevPoints, prevColors, points, colors, 0, 1)
            }

            prevPoints = points
            prevColors = colors
            useA = ! useA
        }
    }

    private fun emitRingQuad(
        buffer: VertexConsumer, pose: PoseStack.Pose,
        prevPoints: FloatArray, prevColors: FloatArray,
        currPoints: FloatArray, currColors: FloatArray,
        j: Int, jn: Int
    ) {
        vertex(buffer, pose, prevPoints, prevColors, j)
        vertex(buffer, pose, prevPoints, prevColors, jn)
        vertex(buffer, pose, currPoints, currColors, jn)

        vertex(buffer, pose, prevPoints, prevColors, j)
        vertex(buffer, pose, currPoints, currColors, jn)
        vertex(buffer, pose, currPoints, currColors, j)
    }

    private fun vertex(buffer: VertexConsumer, pose: PoseStack.Pose, points: FloatArray, colors: FloatArray, idx: Int) {
        val p = idx * 3
        val c = idx * 4
        buffer.addVertex(pose, points[p], points[p + 1], points[p + 2])
            .setColor(colors[c], colors[c + 1], colors[c + 2], colors[c + 3])
    }

    private fun taper(t: Float): Float = (t / TAPER_LENGTH).coerceIn(0f, 1f)

    private fun shapeOffset(shape: BeamShape, t: Float, animTime: Float, amplitude: Float, strand: Int, seed: Int): Pair<Float, Float> {
        return when (shape) {
            BeamShape.STRAIGHT, BeamShape.CYLINDER -> 0f to 0f

            BeamShape.SPIRAL -> {
                val angle = t * SPIRAL_TURNS * TWO_PI + animTime * SPIRAL_ROT_SPEED
                val a = amplitude * taper(t)
                (cos(angle) * a) to (sin(angle) * a)
            }

            BeamShape.DOUBLE_HELIX -> {
                val angle = t * HELIX_TURNS * TWO_PI + animTime * SPIRAL_ROT_SPEED + strand * PI.toFloat()
                val a = amplitude * 0.75f * taper(t)
                (cos(angle) * a) to (sin(angle) * a)
            }

            BeamShape.WAVE -> {
                val a = amplitude * taper(t)
                (sin(t * WAVE_FREQ * TWO_PI + animTime * WAVE_SPEED) * a) to 0f
            }

            BeamShape.RIBBON -> {
                val a = amplitude * taper(t)
                0f to (sin(t * WAVE_FREQ * TWO_PI + animTime * WAVE_SPEED) * a)
            }

            BeamShape.LIGHTNING -> {
                val bucket = floor(animTime * 16f).toInt()
                val segIndex = (t * 64f).toInt()
                val a = amplitude * 1.2f * taper(t)
                val ru = hashFloat(seed, segIndex, bucket)
                val rv = hashFloat(seed + 911, segIndex, bucket)
                (ru * a) to (rv * a * 0.6f)
            }
        }
    }

    private fun tubeRadius(style: BeamStyle, t: Float, animTime: Float, baseRadius: Float): Float {
        var mod = 1f

        if (style.shape == BeamShape.CYLINDER) {
            mod *= 1f + 0.25f * sin(t * PI.toFloat() * 8f - animTime * 10f)
        }

        if (style.pulse) {
            mod *= 1f + 0.18f * sin(animTime * TWO_PI * 1.6f)
        }

        if (style.endpointFade) {
            mod *= (0.4f + 0.6f * taper(t))
        }

        return (baseRadius * mod).coerceAtLeast(0.01f)
    }

    private fun colorAt(style: BeamStyle, t: Float, animTime: Float, alphaMult: Float): Color {
        val edgeFade = if (style.endpointFade) {
            val edge = 0.12f
            minOf((t / edge).coerceIn(0f, 1f), ((1f - t) / edge).coerceIn(0f, 1f))
        }
        else 1f

        val finalAlpha = (style.opacity * alphaMult * edgeFade).coerceIn(0f, 1f)
        val alphaInt = (finalAlpha * 255f).toInt().coerceIn(0, 255)

        return when (style.colorMode) {
            ColorMode.STATIC -> {
                val p = style.primary
                Color(p.red, p.green, p.blue, alphaInt)
            }
            ColorMode.GRADIENT -> {
                val p = style.primary
                val s = style.secondary
                Color(
                    MathUtils.lerp(p.red, s.red, t).toInt(),
                    MathUtils.lerp(p.green, s.green, t).toInt(),
                    MathUtils.lerp(p.blue, s.blue, t).toInt(),
                    alphaInt
                )
            }
            ColorMode.RAINBOW -> {
                val rgb = Color.HSBtoRGB((((t + animTime * 0.18f) % 1f) + 1f) % 1f, 0.85f, 1f)
                Color((rgb and 0xFFFFFF) or (alphaInt shl 24), true)
            }
            ColorMode.CHROMA -> {
                val rgb = Color.HSBtoRGB((((animTime * 0.25f) % 1f) + 1f) % 1f, 0.85f, 1f)
                Color((rgb and 0xFFFFFF) or (alphaInt shl 24), true)
            }
        }
    }

    private fun hashFloat(a: Int, b: Int, c: Int): Float {
        var h = a * 374761393 + b * 668265263 + c * 1274126177
        h = (h xor (h ushr 13)) * - 1640531527
        h = h xor (h ushr 16)
        return (h and 0xFFFFFF) / 8388607.5f - 1f
    }
}