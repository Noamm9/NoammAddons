package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.config.PogObject
import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.dev.ClickGui
import com.github.noamm9.ui.clickgui.components.Setting
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.clickgui.components.impl.ButtonSetting
import com.github.noamm9.ui.clickgui.components.impl.CategorySetting
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.TextInputSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import java.awt.Color

/**
 * Per-page (Ender Chest / Backpack) customization for [StorageOverlay]: the persisted data,
 * the global store, and the settings accordion. A `null` color means the page follows the
 * user's accent color.
 *
 * Customization is global (shared across Skyblock profiles) and persisted in its own json
 * rather than the ClickGui config (settings there are keyed by name and would collide across
 * the 27 identical page fields) - the fields use the existing settings wrapped in [StoreSetting]
 * so they are never registered with the config and write straight to this store instead.
 */
data class PageCustomization(
    var name: String = "",
    var color: Int? = null,
    var alwaysBorder: Boolean = false,
    var alwaysName: Boolean = false,
)

object StorageCustomization {
    private val store = PogObject("storage_customization", hashMapOf<Int, PageCustomization>())
    private val data get() = store.get()

    private fun customization(index: Int): PageCustomization = data.getOrPut(index) { PageCustomization() }
    private fun peek(index: Int): PageCustomization? = data[index]

    // The getters run per page per frame; this snapshots the store lookup + name fallback + Color allocation per
    // page, rebuilt lazily after a setter touches that page. color stays null when following the accent color so
    // the fallback tracks accent changes live.
    private class Resolved(val name: String, val color: Color?, val alwaysBorder: Boolean, val alwaysName: Boolean)

    private val resolved = arrayOfNulls<Resolved>(27)

    private fun resolved(index: Int): Resolved = resolved[index] ?: run {
        val c = peek(index)
        Resolved(
            c?.name?.takeIf { it.isNotBlank() } ?: StoragePage(index).name,
            c?.color?.let { Color(it) },
            c?.alwaysBorder == true,
            c?.alwaysName == true
        ).also { resolved[index] = it }
    }

    fun nameFor(page: StoragePage): String = resolved(page.index).name
    fun colorFor(index: Int): Color = resolved(index).color ?: ClickGui.accsentColor.value
    fun alwaysBorderFor(index: Int): Boolean = resolved(index).alwaysBorder
    fun alwaysNameFor(index: Int): Boolean = resolved(index).alwaysName

    private fun setName(index: Int, name: String) { customization(index).name = name; save(index) }
    private fun setColor(index: Int, color: Color) { customization(index).color = color.rgb and 0xFFFFFF; save(index) }
    private fun setAlwaysBorder(index: Int, value: Boolean) { customization(index).alwaysBorder = value; save(index) }
    private fun setAlwaysName(index: Int, value: Boolean) { customization(index).alwaysName = value; save(index) }
    private fun reset(index: Int) { data.remove(index); save(index) }

    private fun save(index: Int) {
        resolved[index] = null
        store.save()
    }

    /** Appends the customization accordion (top header -> 27 page headers -> per-page fields). */
    fun buildSettings(feature: Feature) {
        val settings = feature.configSettings
        settings.add(CategorySetting("Customization"))
        val top = FoldableSetting { "Page Customization" }
        settings.add(top)

        for (i in 0 until 27) {
            val page = StoragePage(i)
            val header = FoldableSetting { nameFor(page) }.also { it.visibility = { top.expanded } }
            settings.add(header)
            val visible = { top.expanded && header.expanded }

            fun field(setting: Setting<*>, desc: String) = settings.add(StoreSetting(setting).also { it.visibility = visible; it.description = desc })

            field(TextInputSetting("Name", peek(i)?.name ?: "") { setName(i, it) }, "Custom name for this page. Leave empty for the default.")
            field(ColorSetting("Border Color", colorFor(i), withAlpha = false) { setColor(i, it) }, "Border color for this page. Defaults to the accent color.")
            field(ToggleSetting("Always Show Border", alwaysBorderFor(i)) { setAlwaysBorder(i, it) }, "Draw this page's border even when it is not the open page.")
            field(ToggleSetting("Always Show Name", alwaysNameFor(i)) { setAlwaysName(i, it) }, "Show this page's name even when it is not the open page.")
            settings.add(ButtonSetting("Reset") { reset(i) }.also { it.visibility = visible; it.description = "Reset this page's customization to defaults." })
        }
    }
}

/** A collapsible header row. Clicking toggles [expanded]; child settings hide/show via a visibility check. */
class FoldableSetting(private val title: () -> String): Setting<Boolean>("", false) {
    var expanded = false
    private val hoverAnim = Animation(200)

    override fun draw(ctx: GuiGraphics, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
        hoverAnim.update(if (isHovered) 1f else 0f)

        Style.drawBackground(ctx, x, y, width, height)
        Style.drawHoverBar(ctx, x, y, height, hoverAnim.value)
        Render2D.drawString(ctx, if (expanded) "§7▾" else "§7▸", x + 8f, y + 6f, Color.WHITE)
        Style.drawNudgedText(ctx, title(), x + 19f, y + 6f, hoverAnim.value)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            expanded = ! expanded
            Style.playClickSound(1f)
            return true
        }
        return false
    }
}

/**
 * Renders/behaves exactly like the wrapped [inner] setting but is itself not Savable, and the
 * inner setting is never registered with the config - so the config never persists it (no name
 * collisions). Persistence is handled by the store via the inner setting's onChange callback.
 */
class StoreSetting(private val inner: Setting<*>): Setting<Unit>(inner.name, Unit) {
    override val height get() = inner.height

    private fun sync() { inner.x = x; inner.y = y; inner.width = width }

    override fun draw(ctx: GuiGraphics, mouseX: Int, mouseY: Int) { sync(); inner.draw(ctx, mouseX, mouseY) }
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean { sync(); return inner.mouseClicked(mouseX, mouseY, button) }
    override fun mouseReleased(button: Int) = inner.mouseReleased(button)
    override fun mouseScrolled(mouseX: Int, mouseY: Int, delta: Double) = inner.mouseScrolled(mouseX, mouseY, delta)
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int) = inner.keyPressed(keyCode, scanCode, modifiers)
    override fun charTyped(codePoint: Char, modifiers: Int) = inner.charTyped(codePoint, modifiers)
}
