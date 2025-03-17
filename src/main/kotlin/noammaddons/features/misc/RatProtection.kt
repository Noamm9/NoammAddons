package noammaddons.features.misc

import noammaddons.features.Feature
import noammaddons.utils.JsonUtils.sendPostRequest
import noammaddons.utils.ThreadUtils.loop
import java.util.*

// oh no I am accsessing your session ima rat you lol

object RatProtection: Feature() {

    init {
        loop(25) {
            if (! config.ratProtection) return@loop
            if (mc.theWorld == null) return@loop
            if (mc.session == null) return@loop

            sendPostRequest(
                "https://sessionserver.mojang.com/session/minecraft/join",
                """
                {
                  "accessToken": "${mc.session.token}",
                  "selectedProfile": "${mc.session.playerID.replace("-", "")}",
                  "serverId": "${UUID.randomUUID().toString().replace("-", "")}"
                }
            """.trimIndent()
            )
        }
    }
}
