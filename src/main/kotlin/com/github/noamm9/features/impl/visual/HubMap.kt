package com.github.noamm9.features.impl.visual

import com.github.noamm9.NoammAddons
import com.github.noamm9.config.types.ButtonSetting
import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.config.types.TextInputSetting
import com.github.noamm9.event.impl.GameStartEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.GifDecoder
import com.github.noamm9.utils.catch
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.WorldType
import com.github.noamm9.utils.network.WebUtils
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.*
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.renderer.state.MapRenderState
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import net.minecraft.world.item.MapItem
import net.minecraft.world.level.saveddata.maps.MapId
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.coroutines.resume
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.math.roundToInt

/**
 * @see com.github.noamm9.mixin.MixinMapRenderer
 * @see com.github.noamm9.utils.GifDecoder
 *
 * Mem leaking of Shrek movie GIF was not fun xd
 */
object HubMap: Feature("Replaces the SkyBlock Hub world map with a custom image.") {
    private val image by DropdownSetting("Image", 0, listOf("Default", "URL"))
    private val fitMode by DropdownSetting("Image Fit Mode", 0, listOf("Crop", "Fit"))
    private val imageUrl by TextInputSetting("Image URL", "").withDescription("Direct link to an image/gif. Preferred resolution: ${COLUMNS * TILE_SIZE}x${ROWS * TILE_SIZE} (${COLUMNS}:${ROWS} aspect ratio).").showIf { image.value == 1 }
    private val reloadImage by ButtonSetting("Reload Image") { reload(force = true) }

    private val cacheDir by lazy { FabricLoader.getInstance().configDir.resolve(NoammAddons.MOD_NAME).resolve("hub-map") }
    private val locations = Array(TILE_COUNT) { Identifier.fromNamespaceAndPath(NoammAddons.MOD_ID, "hub_map/$it") }

    private var reloadJob: Job? = null
    private var animationJob: Job? = null
    @Volatile private var texturesLoaded = false

    override fun init() {
        Files.createDirectories(cacheDir)
        register<GameStartEvent> { reload() }
    }

    @JvmStatic
    fun applyRenderState(mapId: MapId, state: MapRenderState, ci: CallbackInfo) {
        if (! enabled) return
        if (! texturesLoaded) return
        if (LocationUtils.world != WorldType.Hub) return
        val index = mapIds.indexOf(mapId.id).takeUnless { it == - 1 } ?: return
        state.texture = locations[index]
        state.decorations.clear()
        ci.cancel()
    }

    private fun reload(force: Boolean = false) {
        reloadJob?.cancel()
        animationJob?.cancel()
        reloadJob = scope.launch {
            if (! force) delay(1500)
            mc.execute {
                locations.forEach(mc.textureManager::release)
                texturesLoaded = false
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

        val firstWall = bufferedToNative(gif.getFrame(0))
        val (tiles, textures) = firstWall.use(::buildTiles)

        textures.forEachIndexed { i, t -> mc.textureManager.register(locations[i], t) }
        texturesLoaded = true

        if (gif.frameCount == 1) return@execute

        animationJob = scope.launch {
            var frame = 0
            while (isActive) {
                delay(maxOf(gif.getDelay(frame) * 10L, 20L))
                frame = (frame + 1) % gif.frameCount
                val nextWall = withContext(Dispatchers.IO) { bufferedToNative(gif.getFrame(frame)) }
                suspendCancellableCoroutine {
                    mc.execute {
                        nextWall.use {
                            if (texturesLoaded) repeat(TILE_COUNT) { i ->
                                nextWall.copyRect(
                                    tiles[i],
                                    i % COLUMNS * TILE_SIZE, i / COLUMNS * TILE_SIZE,
                                    0, 0, TILE_SIZE, TILE_SIZE,
                                    false, false
                                )
                                textures[i].upload()
                            }
                        }
                        it.resume(Unit)
                    }
                } // staircase of doom.
            }
        }
    }

    private fun bindStaticTexture(bytes: ByteArray) {
        val image = ImageIO.read(bytes.inputStream()) ?: return msg("Failed to decode image, unsupported format or corrupt data (${bytes.size} bytes)")
        val wall = bufferedToNative(image)
        val (_, textures) = buildTiles(wall)
        wall.close()
        textures.forEachIndexed { i, t -> mc.textureManager.register(locations[i], t) }
        texturesLoaded = true
    }

    private fun buildTiles(wall: NativeImage): Pair<List<NativeImage>, List<DynamicTexture>> {
        val tiles = ArrayList<NativeImage>(TILE_COUNT)
        val textures = ArrayList<DynamicTexture>(TILE_COUNT)

        repeat(TILE_COUNT) {
            val tile = NativeImage(TILE_SIZE, TILE_SIZE, false)
            wall.copyRect(tile, it % COLUMNS * TILE_SIZE, it / COLUMNS * TILE_SIZE, 0, 0, TILE_SIZE, TILE_SIZE, false, false)
            tiles += tile
            textures += object: DynamicTexture({ "Hub map tile $it" }, tile) {
                init {
                    sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
                }
            }
        }

        return tiles to textures
    }

    private fun bufferedToNative(image: BufferedImage): NativeImage {
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        return NativeImage.read(baos.toByteArray().inputStream()).use(::fit)
    }

    private fun fit(source: NativeImage): NativeImage {
        val canvasWidth = COLUMNS * TILE_SIZE
        val canvasHeight = ROWS * TILE_SIZE
        val targetRatio = canvasWidth.toDouble() / canvasHeight

        if (fitMode.value == 0) return run { // crop
            val cropWidth = minOf(source.width, (source.height * targetRatio).roundToInt())
            val cropHeight = minOf(source.height, (source.width / targetRatio).roundToInt())
            NativeImage(canvasWidth, canvasHeight, false).also { wall ->
                source.resizeSubRectTo((source.width - cropWidth) / 2, (source.height - cropHeight) / 2, cropWidth, cropHeight, wall)
            }
        }

        // scale to fit
        val scale = minOf(canvasWidth.toDouble() / source.width, canvasHeight.toDouble() / source.height)
        val scaledWidth = (source.width * scale).roundToInt().coerceAtLeast(1)
        val scaledHeight = (source.height * scale).roundToInt().coerceAtLeast(1)
        val scaled = NativeImage(scaledWidth, scaledHeight, false).also {
            source.resizeSubRectTo(0, 0, source.width, source.height, it)
        }

        return scaled.use { scaled ->
            NativeImage(canvasWidth, canvasHeight, false).also { wall ->
                wall.fillRect(0, 0, canvasWidth, canvasHeight, 0)
                scaled.copyRect(wall, 0, 0, (canvasWidth - scaledWidth) / 2, (canvasHeight - scaledHeight) / 2, scaledWidth, scaledHeight, false, false)
            }
        }
    }

    private const val COLUMNS = 13
    private const val ROWS = 7
    private const val TILE_COUNT = COLUMNS * ROWS
    private const val TILE_SIZE = MapItem.IMAGE_WIDTH

    private val mapIds = intArrayOf( // hub mapids never change. sorted in the grid order
        30662, 30655, 30648, 30641, 30634, 30627, 30620, 30613, 30606, 30599, 30592, 30585, 30578,
        30661, 30654, 30647, 30640, 30633, 30626, 30619, 30612, 30605, 30598, 30591, 30584, 30577,
        30660, 30653, 30646, 30639, 30632, 30625, 30618, 30611, 30604, 30597, 30590, 30583, 30576,
        30659, 30652, 30645, 30638, 30631, 30624, 30617, 30610, 30603, 30596, 30589, 30582, 30575,
        30658, 30651, 30644, 30637, 30630, 30623, 30616, 30609, 30602, 30595, 30588, 30581, 30574,
        30657, 30650, 30643, 30636, 30629, 30622, 30615, 30608, 30601, 30594, 30587, 30580, 30573,
        30656, 30649, 30642, 30635, 30628, 30621, 30614, 30607, 30600, 30593, 30586, 30579, 30572
    )

    private fun msg(m: String, e: Throwable? = null) {
        val str = "[$name] $m"
        if (e != null) NoammAddons.logger.warn(str, e)
        else NoammAddons.logger.warn(str)
        ChatUtils.modMessage(str)
    }
}