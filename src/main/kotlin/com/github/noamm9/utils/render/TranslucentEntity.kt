package com.github.noamm9.utils.render

object TranslucentEntity {
    @JvmField var opacity = 1f

    @JvmStatic fun isActive() = opacity < 1f
}
