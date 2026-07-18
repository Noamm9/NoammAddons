package com.github.noamm9.features.impl.visual

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.ButtonSetting
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.WorldType
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.renderer.state.MapRenderState
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.util.Util
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.level.saveddata.maps.MapId
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.isRegularFile
import kotlin.math.roundToInt

/**
 * @see com.github.noamm9.mixin.MixinMapRenderer
 */
object CustomHubMap: Feature(
    name = "Custom Hub Map",
    description = "Replaces the SkyBlock Hub world map with a custom image.",
) {
    private const val COLUMNS = 13
    private const val ROWS = 7
    private const val TILE_SIZE = 128
    private const val TILE_COUNT = COLUMNS * ROWS

    private val image by DropdownSetting("Image", 0, listOf("Default", "Custom"))
        .onChange { reload() }
    private val reloadImage by ButtonSetting("Reload Image") { reload() }
    private val openFolder by ButtonSetting("Open Image Folder") {
        runCatching { Util.getPlatform().openPath(imageDirectory) }
            .onFailure { NoammAddons.logger.error("Failed to open Hub map image folder", it) }
    }.withDescription("Place hub-map.png in this folder.")
        .showIf { image.value == 1 }

    private val imageDirectory by lazy { FabricLoader.getInstance().configDir.resolve(NoammAddons.MOD_NAME).resolve("hub-map") }
    private val textureIds = Array(TILE_COUNT) { Identifier.fromNamespaceAndPath(NoammAddons.MOD_ID, "hub_map/$it") }

    private var bindings = emptyMap<MapId, Int>()
    private var texturesLoaded = false
    private var reloadRequested = false
    private var nextImageCheck = 0L

    override fun init() {
        runCatching { Files.createDirectories(imageDirectory) }
            .onFailure { NoammAddons.logger.error("Failed to create Hub map image folder", it) }
        register<TickEvent.Start> {
            if (LocationUtils.world != WorldType.Hub) return@register
            val now = Util.getMillis()
            if (now < nextImageCheck) return@register
            nextImageCheck = now + 1000

            if (bindings.size < TILE_COUNT) {
                bindings = mc.level!!.entitiesForRendering().asSequence()
                    .filterIsInstance<ItemFrame>()
                    .mapNotNull { frame ->
                        if (frame.direction != Direction.NORTH) return@mapNotNull null
                        val pos = frame.blockPosition()
                        if (pos.x !in - 6 .. 6 || pos.y !in 69 .. 75 || pos.z != - 6) return@mapNotNull null
                        val id = frame.item.get(DataComponents.MAP_ID) ?: return@mapNotNull null
                        id to (75 - pos.y) * COLUMNS + (6 - pos.x)
                    }
                    .toMap()
            }
            if (! reloadRequested) return@register
            reloadRequested = false

            val source = imageDirectory.resolve("hub-map.png")
                .takeIf { image.value == 1 && it.isRegularFile() }
                ?.let(::readImage)
                ?: readImage()
                ?: return@register
            prepareTextures(source).forEachIndexed { index, texture -> mc.textureManager.register(textureIds[index], texture) }
            texturesLoaded = true
        }
        register<WorldChangeEvent> { bindings = emptyMap() }
    }

    override fun onEnable() {
        super.onEnable()
        reload()
    }

    override fun onDisable() {
        super.onDisable()
        bindings = emptyMap()
        if (texturesLoaded) textureIds.forEach(mc.textureManager::release)
        texturesLoaded = false
    }

    private fun reload() {
        reloadRequested = true
        nextImageCheck = 0L
    }

    @JvmStatic
    fun applyRenderState(mapId: MapId, state: MapRenderState) {
        if (! texturesLoaded) return
        state.texture = textureIds[bindings[mapId] ?: return]
        state.decorations.clear()
    }

    private fun prepareTextures(source: NativeImage): List<DynamicTexture> = source.use { image ->
        val targetRatio = COLUMNS.toDouble() / ROWS
        val cropWidth = minOf(image.width, (image.height * targetRatio).roundToInt())
        val cropHeight = minOf(image.height, (image.width / targetRatio).roundToInt())
        NativeImage(COLUMNS * TILE_SIZE, ROWS * TILE_SIZE, false).use { wall ->
            image.resizeSubRectTo((image.width - cropWidth) / 2, (image.height - cropHeight) / 2, cropWidth, cropHeight, wall)
            val textures = ArrayList<DynamicTexture>(TILE_COUNT)
            try {
                repeat(TILE_COUNT) { textures += createTexture(wall, it) }
                textures
            } catch (error: Exception) {
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
        } catch (error: Exception) {
            tile.close()
            throw error
        }
    }

    private fun readImage(file: Path? = null) = try {
        (file?.inputStream() ?: CustomHubMap::class.java.getResourceAsStream("/assets/noammaddons/icon.png"))?.use(NativeImage::read)
    } catch (error: Exception) {
        NoammAddons.logger.warn("Failed to read Hub map image", error)
        null
    }
}
