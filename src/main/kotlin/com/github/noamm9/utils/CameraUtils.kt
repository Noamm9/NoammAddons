package com.github.noamm9.utils

import com.github.noamm9.mixin.ICamera
import net.minecraft.client.Camera
import net.minecraft.world.phys.Vec3

val Camera.positionVec: Vec3
    get() = (this as ICamera).position
