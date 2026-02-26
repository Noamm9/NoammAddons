package com.github.noamm9.utils.network.data

data class ElectionData(val mayor: Mayor, val minister: Minister? = null) {
    data class Mayor(val name: String, val perks: List<Perk> = emptyList())
    data class Minister(val name: String, val perk: Perk)
    data class Perk(val name: String, val description: String)

    companion object {
        val empty = ElectionData(Mayor(""))
    }
}