package noammaddons.features.dungeons

import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.DungeonEvent.*
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.utils.RenderUtils


object DungeonSecrets: Feature() {
    private data class ClickedSecret(val pos: BlockPos, val time: Long)

    private val secretsClicked = mutableSetOf<ClickedSecret>()
    private var lastPlayed = System.currentTimeMillis()
    private const val SECRET_DISPLAY_TIME = 2000L

    @SubscribeEvent
    fun onSecret(event: SecretEvent) {
        config.takeIf { it.secretSound }?.run {
            if (event.type == SecretEvent.SecretType.ITEM && System.currentTimeMillis() - lastPlayed < 2000) return
            if (event.type == SecretEvent.SecretType.CHEST) lastPlayed = System.currentTimeMillis()
            if (secretsClicked.any { it.pos == event.pos }) return
            config.playSecretSound()
        }

        config.takeIf { it.clickedSecrets }?.let {
            if (secretsClicked.any { it.pos == event.pos }) return
            secretsClicked.add(ClickedSecret(event.pos, System.currentTimeMillis()))
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (! config.clickedSecrets || secretsClicked.isEmpty()) return
        secretsClicked.removeIf { it.time + SECRET_DISPLAY_TIME < System.currentTimeMillis() }

        secretsClicked.forEach { secret ->
            RenderUtils.drawBlockBox(
                secret.pos,
                config.secretClickedColor,
                outline = true,
                fill = true,
                phase = true
            )
        }
    }
}

