package noammaddons.features.impl.dungeons.solvers.terminals.core

import noammaddons.features.impl.dungeons.solvers.terminals.TerminalSolver

enum class ClickMode {
    NORMAL, QUEUE, HOVER, AUTO;

    companion object {
        fun get() = entries[TerminalSolver.clickMode.value]
    }
}