package com.github.noamm9.utils.network.data

import kotlinx.serialization.Serializable

@Serializable
data class RtcaData(val name: String, val runs: Int, val classes: Map<String, Int>)
