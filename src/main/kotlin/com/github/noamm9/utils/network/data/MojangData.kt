package com.github.noamm9.utils.network.data

import com.mojang.authlib.GameProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MojangData(
    val name: String,
    @SerialName("id") val uuid: String
) {
    constructor(profile: GameProfile): this(profile.name, profile.id.toString())
}