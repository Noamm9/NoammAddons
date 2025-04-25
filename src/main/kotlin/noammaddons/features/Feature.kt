package noammaddons.features

import net.minecraft.network.Packet
import net.minecraftforge.common.MinecraftForge
import noammaddons.noammaddons
import noammaddons.ui.config.ConfigGUI
import noammaddons.ui.config.core.CategoryType
import noammaddons.ui.config.core.SubCategory
import noammaddons.ui.config.core.annotations.AlwaysActive
import noammaddons.ui.config.core.annotations.Dev
import noammaddons.ui.config.core.impl.Component
import noammaddons.utils.Utils.spaceCaps


open class Feature(
    val desc: String = "",
    _name: String? = null,
    toggled: Boolean = false,
) {
    val name = _name ?: this::class.simpleName.toString().spaceCaps()

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

    val alwaysActive = this::class.java.isAnnotationPresent(AlwaysActive::class.java)
    private val isDev = this::class.java.isAnnotationPresent(Dev::class.java)

    open val category = if (isDev) CategoryType.DEV else _getCategory() ?: CategoryType.MISC

    open val configSettings: MutableSet<Component<out Any>> = mutableSetOf()

    protected inline val mc get() = noammaddons.mc
    protected inline val config get() = noammaddons.config
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
        }.add(SubCategory(this, configSettings))
    }

    open fun init() {}

    fun addSettings(vararg setting: Component<out Any>) {
        configSettings.addAll(setting)
    }

    fun addSetting(setting: Component<out Any>): Component<out Any> {
        configSettings.add(setting)
        return setting
    }

    private fun _getCategory(): CategoryType? {
        val parts = this::class.java.`package`?.name?.split(".") ?: return null
        val categoryName = parts.getOrNull(parts.indexOf("impl") + 1)?.uppercase() ?: return null
        if (CategoryType.entries.none { it.name == categoryName }) return null
        return CategoryType.valueOf(categoryName)
    }

    open fun onEnable() = MinecraftForge.EVENT_BUS.register(this)

    open fun onDisable() {
        if (! alwaysActive) MinecraftForge.EVENT_BUS.unregister(this)
    }

    fun toggle() {
        enabled = ! enabled
        if (enabled) onEnable()
        else onDisable()
    }

    fun getSettingByName(name: String?): Component<out Any>? {
        return configSettings.find { it.name == name }
    }


    inline fun <reified T: Packet<*>> onPacket(noinline shouldRun: () -> Boolean = { alwaysActive || enabled }, noinline func: (T) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        FeatureManager.packetListeners.add(
            FeatureManager.PacketListener(T::class.java, shouldRun, func) as FeatureManager.PacketListener<Packet<*>>
        )
    }

    fun onChat(filter: Regex, shouldRun: () -> Boolean = { alwaysActive || enabled }, func: (MatchResult) -> Unit) {
        FeatureManager.chatListeners.add(FeatureManager.MessageListener(filter, shouldRun) { matchResult -> func(matchResult) })
    }

    fun onChat(shouldRun: () -> Boolean = { alwaysActive || enabled }, func: (MatchResult) -> Unit) {
        onChat(Regex(".+"), shouldRun, func)
    }

    fun onWorldLoad(func: () -> Unit) {
        FeatureManager.worldLoadListeners.add(func)
    }

    fun onServerTick(shouldRun: () -> Boolean = { alwaysActive || enabled }, func: () -> Unit) {
        FeatureManager.serverTickListeners.add(FeatureManager.ServerTickListener(shouldRun, func))
    }

    enum class TagType {
        NONE, RISKY, FPSTAX
    }
}