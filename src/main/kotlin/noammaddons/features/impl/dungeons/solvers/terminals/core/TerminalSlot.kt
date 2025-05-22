package noammaddons.features.impl.dungeons.solvers.terminals.core

data class TerminalSlot(
    val num: Int,
    val id: Int,
    val meta: Int,
    val size: Int,
    val name: String,
    val enchanted: Boolean
)