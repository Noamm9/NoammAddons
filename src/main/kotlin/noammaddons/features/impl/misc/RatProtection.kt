package noammaddons.features.impl.misc

import noammaddons.features.Feature
import noammaddons.ui.config.core.annotations.Dev
import noammaddons.utils.ThreadUtils.loop
import noammaddons.utils.Utils.remove
import noammaddons.utils.WebUtils
import java.util.*

// oh no I am accsessing your session ima rat you lol

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
