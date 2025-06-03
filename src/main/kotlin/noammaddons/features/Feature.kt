package noammaddons.features

import net.minecraftforge.common.MinecraftForge
import noammaddons.noammaddons
import noammaddons.ui.config.ConfigGUI
import noammaddons.ui.config.core.CategoryType
import noammaddons.ui.config.core.FeatureElement
import noammaddons.ui.config.core.annotations.AlwaysActive
import noammaddons.ui.config.core.annotations.Dev
import noammaddons.ui.config.core.impl.Component
import noammaddons.utils.Utils.spaceCaps


open class Feature(
    val desc: String = "",
    name: String? = null,
    toggled: Boolean = false,
) {
    val name = name ?: this::class.simpleName.toString().spaceCaps()

    @JvmField
    var enabled = toggled

    fun <T: Any> Component<T>.register1(): Component<T> {
        addSetting(this)
        return this
    }

    fun <K: Component<*>> register(setting: K): K {
        @Suppress("UNCHECKED_CAST")
        configSettings.add(setting as Component<out Any>)
        return setting
    }

    private val isDev = this::class.java.isAnnotationPresent(Dev::class.java)
    val alwaysActive = this::class.java.isAnnotationPresent(AlwaysActive::class.java)

    open val category = if (isDev) CategoryType.DEV else _getCategory()

    open val configSettings: MutableSet<Component<out Any>> = mutableSetOf()

    protected inline val mc get() = noammaddons.mc
    protected inline val scope get() = noammaddons.scope
    protected inline val hudData get() = noammaddons.hudData
    protected inline val ahData get() = noammaddons.ahData
    protected inline val bzData get() = noammaddons.bzData
    protected inline val npcData get() = noammaddons.npcData
    protected inline val mayorData get() = noammaddons.mayorData
    protected inline val itemIdToNameLookup get() = noammaddons.itemIdToNameLookup


    fun _init() {
        if (alwaysActive || enabled) onEnable()

        init()

        ConfigGUI.config.getOrPut(category) {
            mutableSetOf()
        }.add(FeatureElement(this, configSettings))
    }

    open fun init() {}

    protected fun addSettings(vararg setting: Component<out Any>) {
        configSettings.addAll(setting)
    }

    protected fun addSetting(setting: Component<out Any>): Component<out Any> {
        configSettings.add(setting)
        return setting
    }

    private fun _getCategory(): CategoryType {
        val parts = this::class.java.`package` !!.name.split(".")
        val categoryName = parts[parts.indexOf("impl") + 1].uppercase()
        if (CategoryType.entries.none { it.name == categoryName }) throw Error("Category does not exist: $categoryName")
        return CategoryType.valueOf(categoryName)
    }

    open fun onEnable() = MinecraftForge.EVENT_BUS.register(this)

    open fun onDisable() {
        if (! alwaysActive) MinecraftForge.EVENT_BUS.unregister(this)
    }

    open fun toggle() {
        enabled = ! enabled
        if (enabled) onEnable()
        else onDisable()
    }

    fun getSettingByName(name: String?): Component<out Any>? {
        return configSettings.find { it.name == name }
    }

    enum class TagType {
        NONE, RISKY, FPSTAX
    }
}