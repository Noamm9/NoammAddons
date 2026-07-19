package com.github.noamm9.features.impl.visual

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.impl.GameStartEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.ButtonSetting
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.TextInputSetting
import com.github.noamm9.utils.MathUtils.destructured
import com.github.noamm9.utils.catch
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.WorldType
import com.github.noamm9.utils.network.WebUtils
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.*
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.renderer.state.MapRenderState
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.level.saveddata.maps.MapId
import net.minecraft.world.phys.AABB
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import javax.imageio.ImageIO
import kotlin.io.path.inputStream
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.math.roundToInt

/**
 * @see com.github.noamm9.mixin.MixinMapRenderer
 */
object CustomHubMap: Feature("Replaces the SkyBlock Hub world map with a custom image.") {
    private val image by DropdownSetting("Image", 0, listOf("Default", "URL")).onChange { reload() }
    private val imageUrl by TextInputSetting("Image URL", "").withDescription("Direct link to an image (PNG/JPEG/GIF/BMP). Preferred resolution: ${COLUMNS * TILE_SIZE}x${ROWS * TILE_SIZE} (${COLUMNS}:${ROWS} aspect ratio).").showIf { image.value == 1 }.onChange { reload() }
    private val reloadImage by ButtonSetting("Reload Image") { reload(force = true) }

    private val cacheDir by lazy { FabricLoader.getInstance().configDir.resolve(NoammAddons.MOD_NAME).resolve("hub-map") }
    private val locations = Array(TILE_COUNT) { Identifier.fromNamespaceAndPath(NoammAddons.MOD_ID, "hub_map/$it") }

    private const val COLUMNS = 13
    private const val ROWS = 7
    private const val TILE_SIZE = 128
    private const val TILE_COUNT = COLUMNS * ROWS
    private val PNG_SIGNATURE = intArrayOf(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A).map(Int::toByte)
    private var reloadJob: Job? = null
    private val frameBox = AABB(7.0, 77.0, - 6.0, - 8.0, 68.0, - 4.0)

    private val bindings = mutableMapOf<MapId, Int>()
    @Volatile private var texturesLoaded = false
    private var nextCheck = 0L

    override fun init() {
        Files.createDirectories(cacheDir)

        register<GameStartEvent> { reload() }
        register<TickEvent.Start> {
            if (LocationUtils.world != WorldType.Hub) return@register
            val now = System.currentTimeMillis()
            if (now < nextCheck) return@register
            nextCheck = now + 1000

            bindings.putAll(mc.level?.getEntities(null, frameBox)?.mapNotNull { entity ->
                val frame = entity as? ItemFrame ?: return@mapNotNull null
                if (frame.direction != Direction.NORTH) return@mapNotNull null
                val (x, y, z) = frame.blockPosition().destructured()
                if (x !in - 6 .. 6 || y !in 69 .. 75 || z != - 6) return@mapNotNull null
                val id = frame.item.get(DataComponents.MAP_ID) ?: return@mapNotNull null
                id to (75 - y) * COLUMNS + (6 - x)
            }?.toMap().orEmpty())
        }
    }

    @JvmStatic
    fun applyRenderState(mapId: MapId, state: MapRenderState, ci: CallbackInfo) {
        if (! enabled) return
        if (! texturesLoaded) return
        state.texture = locations[bindings[mapId] ?: return]
        state.decorations.clear()
        ci.cancel()
    }

    private fun reload(force: Boolean = false) {
        reloadJob?.cancel()
        reloadJob = scope.launch {
            delay(1500)
            mc.execute {
                locations.forEach(mc.textureManager::release)
                texturesLoaded = false
            }
            if (image.value == 0) readImage()?.let(::applyTextures)
            else loadFromUrl(imageUrl.value, force)
        }
    }

    private suspend fun loadFromUrl(url: String, force: Boolean) {
        val uri = catch { URI(url) } ?: return
        if (! uri.scheme.equalsOneOf("http", "https")) return
        if (uri.host.isNullOrBlank()) return

        val cacheFile = cacheDir.resolve(cacheKeyFor(url))
        if (! force && cacheFile.isRegularFile()) {
            readImage(cacheFile)?.let(::applyTextures)
            NoammAddons.logger.info("image loaded from cache.")
            return
        }

        val response = WebUtils.get(url).getOrThrow()
        if (! response.status.isSuccess()) return NoammAddons.logger.warn("Failed to download Hub map (HTTP ${response.status})")
        val bytes = response.readRawBytes()

        withContext(Dispatchers.IO) {
            val source = decodeImage(bytes) ?: run {
                val contentType = response.headers[HttpHeaders.ContentType] ?: "unknown"
                NoammAddons.logger.error(
                    "Hub map URL returned an unsupported or corrupt image (Content-Type: $contentType, ${bytes.size} bytes). " +
                        "Supported formats: PNG, JPEG, GIF, BMP. WebP is not supported. URL: $url"
                )
                texturesLoaded = true
                return@withContext
            }

            source.writeToFile(cacheFile)
            applyTextures(source)
        }
    }

    private fun decodeImage(bytes: ByteArray): NativeImage? {
        if (isPNG(bytes)) return catch { NativeImage.read(ByteArrayInputStream(bytes)) }
        return NativeImage.read(ByteArrayOutputStream().also {
            ImageIO.write(ImageIO.read(bytes.inputStream()), "png", it)
        }.toByteArray().inputStream())
    }

    private fun isPNG(bytes: ByteArray): Boolean {
        if (bytes.size < PNG_SIGNATURE.size) return false
        return PNG_SIGNATURE.indices.all { bytes[it] == PNG_SIGNATURE[it] }
    }

    private fun cacheKeyFor(url: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
        return digest.joinToString("") { "%02x".format(it) } + ".png"
    }

    private fun applyTextures(source: NativeImage) = mc.execute {
        prepareTextures(source).forEachIndexed { i, t ->
            mc.textureManager.register(locations[i], t)
        }
        texturesLoaded = true
    }

    private fun prepareTextures(source: NativeImage) = source.use { image ->
        val targetRatio = COLUMNS.toDouble() / ROWS
        val cropWidth = minOf(image.width, (image.height * targetRatio).roundToInt())
        val cropHeight = minOf(image.height, (image.width / targetRatio).roundToInt())

        NativeImage(COLUMNS * TILE_SIZE, ROWS * TILE_SIZE, false).use { wall ->
            image.resizeSubRectTo((image.width - cropWidth) / 2, (image.height - cropHeight) / 2, cropWidth, cropHeight, wall)
            val textures = ArrayList<DynamicTexture>(TILE_COUNT)
            try {
                repeat(TILE_COUNT) { textures += createTexture(wall, it) }
                textures
            }
            catch (error: Exception) {
                textures.forEach(DynamicTexture::close)
                throw error
            }
        }
    }

    private fun createTexture(wall: NativeImage, index: Int): DynamicTexture {
        val tile = NativeImage(TILE_SIZE, TILE_SIZE, false)
        return try {
            wall.copyRect(tile, index % COLUMNS * TILE_SIZE, index / COLUMNS * TILE_SIZE, 0, 0, TILE_SIZE, TILE_SIZE, false, false)
            object: DynamicTexture({ "Hub map tile $index" }, tile) {
                init {
                    sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
                }
            }
        }
        catch (error: Exception) {
            tile.close()
            throw error
        }
    }

    private fun readImage(file: Path? = null) = try {
        (file?.inputStream() ?: this::class.java.getResourceAsStream("/assets/noammaddons/hub-map.png"))?.use(NativeImage::read)
    }
    catch (error: Exception) {
        NoammAddons.logger.info("Failed to read image. ${file?.name}, $file", error)
        null
    }
}