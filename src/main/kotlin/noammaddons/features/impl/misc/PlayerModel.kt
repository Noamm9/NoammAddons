package noammaddons.features.impl.misc

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraftforge.client.event.RenderPlayerEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.DungeonUtils.dungeonTeammates
import noammaddons.utils.LocationUtils.inDungeon

object PlayerModel: Feature() {
    private val playerScale = ToggleSetting("Custom Scale")
    private val scaleEveryone = ToggleSetting("Scale Everyone").addDependency(playerScale)
    private val pScale = SliderSetting("Scale Factor", 0.1f, 2f, 0.1f, 1f).addDependency(playerScale)

    private val playerSpin = ToggleSetting("Player Spin")
    private val spinSpeed = SliderSetting("Spin Speed", 1, 25, 1, 10.0).addDependency(playerSpin)
    private val spinDirection = DropdownSetting("Spin Direction", listOf("Left", "Right")).addDependency(playerSpin)
    private val spinOnEveryone = ToggleSetting("Spin On Everyone").addDependency(playerSpin)
    private val speedFactor get() = spinSpeed.value

    override fun init() = addSettings(
        SeperatorSetting("Scale"),
        playerScale, scaleEveryone, pScale,
        SeperatorSetting("Spin"),
        playerSpin, spinOnEveryone,
        spinDirection, spinSpeed,
    )

    private fun getRotation(): Float {
        val millis = System.currentTimeMillis() % 4000
        val fraction = millis / 4000f
        val angle = (fraction * 360f) * speedFactor.toFloat()
        return if (spinDirection.value != 0) angle - 180f else 180f - angle
    }


    @SubscribeEvent
    fun onRenderEntityPre(event: RenderPlayerEvent.Pre) {
        val applyScale = playerScale.value && (scaleEveryone.value || event.entity == mc.thePlayer)
                && (! inDungeon || dungeonTeammates.toList().any { it.entity == event.entity })

        val applySpin = playerSpin.value && (spinOnEveryone.value || event.entity == mc.thePlayer)

        if (applyScale || applySpin) {
            GlStateManager.pushMatrix()
            GlStateManager.translate(event.x, event.y, event.z)

            if (applyScale) GlStateManager.scale(pScale.value, pScale.value, pScale.value)
            if (applySpin) GlStateManager.rotate(getRotation(), 0f, 1f, 0f)

            GlStateManager.translate(- event.x, - event.y, - event.z)
        }
    }

    @SubscribeEvent
    fun onRenderEntityPost(event: RenderPlayerEvent.Post) {
        if (
            (playerScale.value && (scaleEveryone.value || event.entity == mc.thePlayer)) ||
            (playerSpin.value && (spinOnEveryone.value || event.entity == mc.thePlayer))
        ) {
            if (! inDungeon || dungeonTeammates.toList().any { it.entity == event.entity }) {
                GlStateManager.popMatrix()
            }
        }
    }

    // IDK why but this shit legit gave me a headache
    fun getPlayerScaleFactor(ent: Entity): Float {
        if (! playerScale.value || ! enabled) return 1f
        return if (scaleEveryone.value) pScale.value
        else if (ent == mc.thePlayer) pScale.value
        else 1f
    }
}