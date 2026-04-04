package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.dungeons.DungeonProfileSummary
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import java.util.Locale

data class PartyFinderRuleConfig(
    val floor: Int,
    val masterMode: Boolean,
    val enforcePbLimit: Boolean = true,
    val pbLimitSeconds: Int,
    val minimumSecretsThousands: Int = 0,
    val minimumSecretsPerRun: Double = 0.0,
)

object PartyFinderRules {
    fun duplicateClassReason(joiningClass: DungeonClass, currentClasses: Collection<DungeonClass>): String? {
        if (joiningClass == DungeonClass.Empty) return null
        return if (currentClasses.any { it == joiningClass }) "Dupe(${joiningClass.name})"
        else null
    }

    fun evaluate(summary: DungeonProfileSummary, config: PartyFinderRuleConfig): List<String> {
        val reasons = mutableListOf<String>()

        if (config.enforcePbLimit) {
            val pbLimit = formatDuration(config.pbLimitSeconds * 1000L)
            val pbMilliseconds = summary.floorPbMilliseconds

            if (pbMilliseconds == null) {
                reasons.add("PB(No S+/$pbLimit)")
            }
            else if (pbMilliseconds / 1000 > config.pbLimitSeconds) {
                val floorPrefix = if (config.masterMode) "M" else "F"
                reasons.add("$floorPrefix${config.floor}: PB(${formatDuration(pbMilliseconds)}/$pbLimit)")
            }
        }

        if (config.minimumSecretsThousands > 0) {
            val secrets = summary.totalSecrets ?: 0
            if (secrets < config.minimumSecretsThousands * 1000) {
                reasons.add("Secrets(${secrets / 1000}k/${config.minimumSecretsThousands}k)")
            }
        }

        if (config.minimumSecretsPerRun > 0) {
            val secretsPerRun = summary.secretsPerRun ?: 0.0
            if (secretsPerRun < config.minimumSecretsPerRun) {
                reasons.add("SPR(${secretsPerRun.toFixed(2)}/${config.minimumSecretsPerRun.toFixed(2)})")
            }
        }

        return reasons
    }

    fun formatDuration(milliseconds: Number): String {
        val totalSeconds = milliseconds.toLong() / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        }
        else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }
}
