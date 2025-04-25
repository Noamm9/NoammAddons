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
    private val playerScale = ToggleSetting("Custom Scale Scale")
    private val scaleEveryone = ToggleSetting("Scale Everyone").addDependency(playerScale)
    private val pScale = SliderSetting("Scale", 0.1, 2, 1.0).addDependency(playerScale)

    private val playerSpin = ToggleSetting("Player Spin")
    private val spinSpeed = SliderSetting("Spin Speed", 1, 25, 10.0).addDependency(playerSpin)
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
        val entity = event.entity

        val applyScale = playerScale.value &&
                (scaleEveryone.value || entity == mc.thePlayer) &&
                (! inDungeon || dungeonTeammates.toList().any { it.entity == entity })

        val applySpin = playerSpin.value &&
                (spinOnEveryone.value || entity == mc.thePlayer)

        if (applyScale || applySpin) {
            GlStateManager.pushMatrix()
            GlStateManager.translate(event.x, event.y, event.z)

            if (applyScale) {
                val scale = pScale.value.toFloat()
                GlStateManager.scale(scale, scale, scale)
            }

            if (applySpin) {
                GlStateManager.rotate(getRotation(), 0f, 1f, 0f)
            }

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
        if (! playerScale.value) return 1f
        return if (scaleEveryone.value) pScale.value.toFloat()
        else if (ent == mc.thePlayer) pScale.value.toFloat()
        else 1f
    }
}