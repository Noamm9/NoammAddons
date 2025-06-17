package noammaddons.features.impl.misc

import noammaddons.features.Feature
import noammaddons.ui.config.core.annotations.Dev
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ThreadUtils.loop
import noammaddons.utils.Utils.remove
import noammaddons.utils.WebUtils
import java.io.IOException
import java.net.*
import java.util.*


@Dev
object RatProtection: Feature() {
    init {
        loop(25) {
            if (! enabled) return@loop
            if (mc.theWorld == null) return@loop
            if (mc.session == null) return@loop

            WebUtils.sendPostRequest(
                "https://sessionserver.mojang.com/session/minecraft/join",
                """
                {
                  "accessToken": "${mc.session.token}",
                  "selectedProfile": "${mc.session.playerID.remove("-")}",
                  "serverId": "${UUID.randomUUID().toString().remove("-")}"
                }
            """.trimIndent()
            )
        }
    }
}


object RatHttpInterceptor {
    private val suspiciousEndpoints = setOf(
        "discord",
        "webhooks",
        "pastebin.com/api",
        "hastebin.com",
        "paste.ee/api",
        "api.github.com/gists",
        "requestbin.com",
        "postman-echo.com"
    )

    private lateinit var proxySelector: ProxySelector

    fun install() {
        val default = ProxySelector.getDefault()

        proxySelector = object: ProxySelector() {
            override fun select(uri: URI): List<Proxy> {
                val url = uri.toString()
                if (isSuspicious(url)) {
                    modMessage("Rat Protection >> &c&lSuspicious request: &r&c$url")
                    return listOf(Proxy(Proxy.Type.HTTP, InetSocketAddress("localhost", 0)))
                }
                return default?.select(uri) ?: listOf(Proxy.NO_PROXY)
            }

            override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
                default?.connectFailed(uri, sa, ioe)
            }

            private fun isSuspicious(url: String): Boolean {
                return suspiciousEndpoints.any { url.contains(it, ignoreCase = true) }
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