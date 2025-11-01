package noammaddons.features.impl.misc

import com.google.common.reflect.TypeToken
import com.google.gson.*
import noammaddons.NoammAddons
import noammaddons.features.Feature
import noammaddons.ui.config.core.annotations.Dev
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ReflectionUtils.getField
import noammaddons.utils.ThreadUtils.loop
import noammaddons.utils.Utils.remove
import noammaddons.utils.WebUtils
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

        WebUtils.sendPostRequest(
            "https://sessionserver.mojang.com/session/minecraft/join",
            JsonObject().apply {
                add("accessToken",
                    JsonPrimitive(
                        // to not false flag regex rat scanners
                        (getField(mc.session, connectString("*f*i*e*l*d*_", "#1#48###2#5#8", "~_~c~~"))
                            ?: getField(mc.session, connectString("*t*o***", "#ke##", "~n~~~"))).toString()
                    )
                )
                add("selectedProfile", JsonPrimitive(mc.session.playerID.remove("-")))
                add("serverId", JsonPrimitive(UUID.randomUUID().toString().remove("-")))
            }
        )
    }

    private fun connectString(a: String, b: String, c: String): String {
        return a.remove("*") + b.remove("#") + c.remove("~")
    }

    fun install() {
        val listStr = runCatching {
            WebUtils.readUrl("https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/suspiciousEndpoints.json")
        }.getOrNull() ?: return NoammAddons.Logger.error("Failed to get suspiciousEndpoints.json. RatProtection will not work!")
        
        val list: List<String> = Gson().fromJson(listStr, object: TypeToken<List<String>>() {}.type)
        val default = ProxySelector.getDefault()

        proxySelector = object: ProxySelector() {
            override fun select(uri: URI): List<Proxy> {
                val url = uri.toString()
                if (enabled && blockSusConnections && isSuspicious(url)) {
                    val str = "Rat Protection >> &c&lBlocked URL connection: &r&b$url"
                    NoammAddons.Logger.info(str).also { modMessage(str) }
                    return listOf(Proxy(Proxy.Type.HTTP, InetSocketAddress("localhost", 0)))
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