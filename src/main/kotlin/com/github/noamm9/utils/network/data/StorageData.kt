package com.github.noamm9.utils.network.data

import com.github.noamm9.utils.network.ApiUtils
import kotlinx.serialization.Serializable

@Serializable
data class StorageData(
    private val ender_chest_contents: String,
    private val backpack_contents: Map<String, String>
) {
    val backpack by lazy { backpack_contents.map { it.key.toInt() to ApiUtils.decodeBase64ItemList(it.value) }.toMap() }
    val enderchest by lazy { ApiUtils.decodeBase64ItemList(ender_chest_contents).chunked(45).withIndex().associate { it.index to it.value } }
}