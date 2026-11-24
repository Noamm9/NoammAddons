package com.github.noamm9.features.impl.visual

import com.github.noamm9.NoammAddons
import com.github.noamm9.config.types.*
import com.github.noamm9.event.impl.GameStartEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.*
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.WorldType
import com.github.noamm9.utils.network.WebUtils
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.vertex.PoseStack
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.*
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.network.chat.FontDescription
import net.minecraft.resources.Identifier
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.entity.Display
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.awt.image.BufferedImage
import java.net.URI
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.coroutines.resume
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.math.roundToInt

/**
 * @see com.github.noamm9.mixin.MixinTextDisplayRenderer
 * @see com.github.noamm9.utils.GifDecoder
 *
 * Mem leaking of Shrek movie GIF was not fun xd
 */
object HubMap: Feature("Replaces the SkyBlock Hub world map with a custom image.") {
    private val image by DropdownSetting("Image", 0, listOf("Default", "URL"))
    private val fitMode by DropdownSetting("Image Fit Mode", 0, listOf("Crop", "Fit"))
    private val imageUrl by TextInputSetting("Image URL", "").withDescription("Direct link to an image/gif. Preferred resolution: ${WIDTH}x${HEIGHT} (13:7 aspect ratio).").showIf { image.value == 1 }
    private val reloadImage by ButtonSetting("Reload Image") { reload(force = true) }

    // values from hypixel pack at font/map.json
    private const val WIDTH = 1664
    private const val HEIGHT = 896
    private const val LINE_HEIGHT = 10
    private const val GLYPH_ASCENT = 208
    private const val GLYPH_HEIGHT = 210

    private val cacheDir by lazy { FabricLoader.getInstance().configDir.resolve(NoammAddons.MOD_NAME).resolve("hub-map") }
    private val location = Identifier.fromNamespaceAndPath(NoammAddons.MOD_ID, "hub_map")
    private val mapFont = FontDescription.Resource(Identifier.fromNamespaceAndPath("hypixel_skyblock", "map"))
    private val renderType by lazy { RenderTypes.textPolygonOffset(location) }

    private var mapInfo: Display.TextDisplay.CachedInfo? = null
    private var firstMapLine = 0
    private var lastMapLine = 0

    private var reloadJob: Job? = null
    private var animationJob: Job? = null
    @Volatile private var textureLoaded = false

    override fun init() {
        Files.createDirectories(cacheDir)
        register<GameStartEvent> { reload() }
    }

    @JvmStatic
    fun submitTextDisplay(state: TextDisplayEntityRenderState, poseStack: PoseStack, collector: SubmitNodeCollector, lightCoords: Int, ci: CallbackInfo) {
        if (! enabled) return
        if (! textureLoaded) return
        if (LocationUtils.world != WorldType.Hub) return
        val renderState = state.textRenderState ?: return
        val info = state.cachedInfo ?: return
        val lines = info.lines

        if (info !== mapInfo) {
            val first = lines.indexOfFirst { it.contents.firstFont() != null }.takeUnless { it == - 1 } ?: return
            if (lines[first].contents.firstFont() != mapFont) return
            mapInfo = info
            firstMapLine = first
            lastMapLine = lines.indexOfLast { it.contents.firstFont() != null }
        }

        val first = firstMapLine
        val last = lastMapLine
        ci.cancel()

        poseStack.last().pose().apply {
            rotate(Math.PI.toFloat(), 0f, 1f, 0f)
            scale(- 0.025f)
            translate(1f - info.width / 2f, - (lines.size * LINE_HEIGHT - 1f), 0f)
        }

        val line = lines[first]
        val x0 = when (Display.TextDisplay.getAlign(renderState.flags)) {
            Display.TextDisplay.Align.LEFT -> 0f
            Display.TextDisplay.Align.RIGHT -> (info.width - line.width).toFloat()
            Display.TextDisplay.Align.CENTER -> info.width / 2f - line.width / 2f
        }

        val x1 = x0 + line.width
        val y0 = first * LINE_HEIGHT + 7f - GLYPH_ASCENT
        val y1 = last * LINE_HEIGHT + 7f - GLYPH_ASCENT + GLYPH_HEIGHT

        collector.submitCustomGeometry(poseStack, renderType) { pose, buffer ->
            buffer.addVertex(pose, x0, y0, 0.1f).setColor(- 1).setUv(0f, 0f).setLight(lightCoords)
            buffer.addVertex(pose, x0, y1, 0.1f).setColor(- 1).setUv(0f, 1f).setLight(lightCoords)
            buffer.addVertex(pose, x1, y1, 0.1f).setColor(- 1).setUv(1f, 1f).setLight(lightCoords)
            buffer.addVertex(pose, x1, y0, 0.1f).setColor(- 1).setUv(1f, 0f).setLight(lightCoords)
        }
    }

    private fun FormattedCharSequence.firstFont(): FontDescription? {
        var font: FontDescription? = null
        accept { _, style, _ ->
            font = style.font
            false
        }
        return font
    }

    private fun reload(force: Boolean = false) {
        reloadJob?.cancel()
        animationJob?.cancel()
        reloadJob = scope.launch {
            if (! force) delay(1500)
            mc.execute {
                mc.textureManager.release(location)
                textureLoaded = false
            }
            if (image.value == 0) loadDefault()
            else loadFromUrl(imageUrl.value, force)
        }
    }

    private fun loadDefault() {
        HubMap::class.java.getResourceAsStream("/assets/noammaddons/hub-map.png")?.readBytes()?.let(::bindTexture)
    }

    private suspend fun loadFromUrl(url: String, force: Boolean) {
        if (url.isBlank()) return
        val uri = catch { URI(url) } ?: return
        if (! uri.scheme.equalsOneOf("http", "https")) return
        if (uri.host.isNullOrBlank()) return

        val cacheFile = cacheDir.resolve(url.hashCode().toString())
        if (! force && cacheFile.isRegularFile()) {
            cacheFile.readBytes().let(::bindTexture)
            return
        }

        val response = WebUtils.get(url).getOrThrow()
        if (! response.status.isSuccess()) return msg("Failed to download (HTTP ${response.status})")

        val contentType = response.headers["Content-Type"]
        if (contentType != null && ! contentType.startsWith("image/") && contentType != "application/octet-stream") return msg(
            "URL didn't return an image (got '$contentType'). Discord/Tenor embed links often point to video. use the direct file link instead."
        )

        val bytes = response.readRawBytes()
        withContext(Dispatchers.IO) {
            Files.write(cacheFile, bytes)
            bindTexture(bytes)
        }
    }

    private fun bindTexture(bytes: ByteArray) = mc.execute {
        val gif = catch { GifDecoder.read(bytes) } ?: return@execute bindStaticTexture(bytes)

        if (gif.frameCount == 0) return@execute print("Failed to load. GIF has 0 frames")

        val wall = bufferedToNative(gif.getFrame(0))
        val texture = registerTexture(wall)

        if (gif.frameCount == 1) return@execute

        animationJob = scope.launch {
            var frame = 0
            var deadline = System.currentTimeMillis() + gif.frameDelay(frame)
            while (isActive) {
                frame = (frame + 1) % gif.frameCount
                val nextWall = withContext(Dispatchers.IO) { bufferedToNative(gif.getFrame(frame)) }
                delay(deadline - System.currentTimeMillis())
                // deadline based so decode/upload time wont add on top of the frame delay
                deadline = maxOf(deadline + gif.frameDelay(frame), System.currentTimeMillis())
                suspendCancellableCoroutine {
                    mc.execute {
                        nextWall.use {
                            if (textureLoaded) {
                                nextWall.copyRect(wall, 0, 0, 0, 0, WIDTH, HEIGHT, false, false)
                                texture.upload()
                            }
                        }
                        it.resume(Unit)
                    }
                } // staircase of doom.
            }
        }
    }

    private fun GifDecoder.GifImage.frameDelay(frame: Int) = getDelay(frame) * 10L

    private fun bindStaticTexture(bytes: ByteArray) {
        val image = ImageIO.read(bytes.inputStream()) ?: return msg("Failed to decode image, unsupported format or corrupt data (${bytes.size} bytes)")
        registerTexture(bufferedToNative(image))
    }

    private fun registerTexture(wall: NativeImage): DynamicTexture {
        val texture = DynamicTexture({ "Hub map" }, wall)
        mc.textureManager.register(location, texture)
        textureLoaded = true
        return texture
    }

    private fun bufferedToNative(image: BufferedImage): NativeImage {
        val pixels = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
        return NativeImage(image.width, image.height, false).apply {
            for (y in 0 until image.height) for (x in 0 until image.width) {
                setPixel(x, y, pixels[y * image.width + x])
            }
        }.use(::fit)
    }

    private fun fit(source: NativeImage): NativeImage {
        if (fitMode.value == 0) return run { // crop
            val targetRatio = WIDTH.toDouble() / HEIGHT
            val cropWidth = minOf(source.width, (source.height * targetRatio).roundToInt())
            val cropHeight = minOf(source.height, (source.width / targetRatio).roundToInt())
            NativeImage(WIDTH, HEIGHT, false).also { wall ->
                source.resizeSubRectTo((source.width - cropWidth) / 2, (source.height - cropHeight) / 2, cropWidth, cropHeight, wall)
            }
        }

        // scale to fit
        val scale = minOf(WIDTH.toDouble() / source.width, HEIGHT.toDouble() / source.height)
        val scaledWidth = (source.width * scale).roundToInt().coerceAtLeast(1)
        val scaledHeight = (source.height * scale).roundToInt().coerceAtLeast(1)
        val scaled = NativeImage(scaledWidth, scaledHeight, false).also {
            source.resizeSubRectTo(0, 0, source.width, source.height, it)
        }

        return scaled.use { scaled ->
            NativeImage(WIDTH, HEIGHT, false).also { wall ->
                wall.fillRect(0, 0, WIDTH, HEIGHT, 0)
                scaled.copyRect(wall, 0, 0, (WIDTH - scaledWidth) / 2, (HEIGHT - scaledHeight) / 2, scaledWidth, scaledHeight, false, false)
            }
        }
    }

    private fun msg(m: String, e: Throwable? = null) {
        val str = "[$name] $m"
        if (e != null) NoammAddons.logger.warn(str, e)
        else NoammAddons.logger.warn(str)
        ChatUtils.modMessage(str)
    }
}