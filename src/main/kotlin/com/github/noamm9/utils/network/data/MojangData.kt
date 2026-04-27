package com.github.noamm9.utils.network.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MojangData(val name: String, @SerialName("id") val uuid: String)