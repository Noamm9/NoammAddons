package noammaddons.features.impl.misc

import noammaddons.NoammAddons
import noammaddons.features.Feature
import noammaddons.ui.config.core.annotations.Dev
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils
import noammaddons.utils.DataDownloader
import noammaddons.utils.ThreadUtils.loop
import noammaddons.utils.Utils.remove
import java.io.IOException
import java.net.*
import java.util.*


@Dev
object RatProtection: Feature() {
    private val blockEndPoint by ToggleSetting("Block Mojang Endpoint")
    private val blockSusConnections by ToggleSetting("Block Suspicious Connections ", true)
    private lateinit var proxySelector: ProxySelector

    override fun init() = loop(1000) {
        if (! enabled) return@loop
        if (! blockEndPoint) return@loop
        if (mc.theWorld == null) return@loop
        if (mc.session == null) return@loop

        runCatching {
            mc.sessionService.joinServer(mc.session.profile, mc.session.token, UUID.randomUUID().toString().remove("-"))
        }
    }

    fun install() {
        val list = DataDownloader.loadJson<List<String>>("suspiciousEndpoints.json")
        val default = ProxySelector.getDefault()

        proxySelector = object: ProxySelector() {
            override fun select(uri: URI): List<Proxy> {
                val url = uri.toString()
                if (enabled && blockSusConnections && isSuspicious(url)) {
                    val str = "Rat Protection >> &c&lBlocked URL: &r&b$url"
                    NoammAddons.Logger.info(str)
                    ChatUtils.modMessage(str)
                    throw SecurityException("Connection blocked by Rat Protection: $url")
                }
                return default?.select(uri) ?: listOf(Proxy.NO_PROXY)
            }

            override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
                default?.connectFailed(uri, sa, ioe)
            }

            private fun isSuspicious(url: String): Boolean {
                return list.any { url.contains(it, ignoreCase = true) }
            }
        }

        ProxySelector.setDefault(proxySelector)
        lockSelector()
    }

    private fun lockSelector() = loop(5000) {
        if (ProxySelector.getDefault() == proxySelector) return@loop
        ProxySelector.setDefault(proxySelector)
    }
}