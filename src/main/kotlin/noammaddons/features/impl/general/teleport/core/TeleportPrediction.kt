package noammaddons.features.impl.general.teleport.core

import net.minecraft.util.Vec3
import noammaddons.utils.MathUtils

data class TeleportPrediction(val rotation: MathUtils.Rotation, val position: Vec3, val info: TeleportInfo)
