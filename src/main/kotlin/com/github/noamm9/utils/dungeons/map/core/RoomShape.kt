package com.github.noamm9.utils.dungeons.map.core

import com.google.gson.annotations.SerializedName

enum class RoomShape(val tileCount: Int) {
    @SerializedName("Unknown") UNKNOWN(0),
    @SerializedName("L") SL(3),
    @SerializedName("1x1") S1x1(1),
    @SerializedName("1x2") S2x1(2),
    @SerializedName("1x3") S3x1(3),
    @SerializedName("1x4") S4x1(4),
    @SerializedName("2x2") S2x2(4);
}