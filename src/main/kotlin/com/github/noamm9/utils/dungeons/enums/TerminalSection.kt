package com.github.noamm9.utils.dungeons.enums

import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.utils.dungeons.DungeonListener

enum class TerminalSection(val taskCount: Int, val reqTerminals: Int) {
    UNKNOWN(0, 0),
    S1(7, 4),
    S2(8, 5),
    S3(7, 4),
    S4(7, 4),
    GOLDOR(0, 0);

    enum class Task { TERMINAL, DEVICE, LEVER }

    var terminals = 0
        private set

    var levers = 0
        private set

    var device = false
        private set

    private var _gate = false

    val gate get() = this == S4 || _gate

    var startTime = DungeonListener.DualTime(0L)
        private set

    var endTime = DungeonListener.DualTime(0L)
        private set

    var complatedTasks = 0
        private set

    val completed: Boolean get() = complatedTasks >= taskCount && gate

    fun process(message: String): TerminalSection {
        TaskCompletedRegex.find(message)?.destructured?.let { (playerName, _, typeStr, currentStr, _) ->
            val player = DungeonListener.dungeonTeammates.find { it.name.equals(playerName, true) }
            val type = Task.valueOf(typeStr.uppercase())
            complatedTasks = currentStr.toInt()

            when (type) {
                Task.TERMINAL -> terminals ++
                Task.LEVER -> levers ++
                Task.DEVICE -> device = true
            }

            EventBus.post(DungeonEvent.TerminalStageEvent.Task(this, type, player))
        }

        if (message == "The gate has been destroyed!") {
            _gate = true
            EventBus.post(DungeonEvent.TerminalStageEvent.GateDestroyed(this))
        }

        return if (completed && endTime.ticks == 0L) {
            endTime = DungeonListener.DualTime(DungeonListener.currentTime)
            EventBus.post(DungeonEvent.TerminalStageEvent.End(this))
            when (this) {
                S1 -> S2
                S2 -> S3
                S3 -> S4
                S4 -> GOLDOR
                else -> return this
            }.start()
        }
        else this
    }

    fun start() = apply {
        if (ordinal !in 1 .. 5) return@apply
        if (this == S1) TerminalSection.resetAll()
        startTime = DungeonListener.DualTime(DungeonListener.currentTime)
        EventBus.post(DungeonEvent.TerminalStageEvent.Start(this))
    }

    fun reset() {
        complatedTasks = 0

        terminals = 0
        levers = 0
        device = false
        _gate = false

        startTime = DungeonListener.DualTime(0L)
        endTime = DungeonListener.DualTime(0L)
    }

    companion object {
        private val TaskCompletedRegex = Regex("^(\\w{1,16}) (activated|completed) a (terminal|lever|device)! \\((\\d)/(\\d)\\)$")
        fun resetAll() = entries.forEach(TerminalSection::reset)
    }
}