package com.github.noamm9.config

import com.google.gson.JsonObject

abstract class ConfigMigrator(val fromVersion: Int, val toVersion: Int) {
    init {
        require(toVersion > fromVersion) { "toVersion must be greater than fromVersion" }
    }

    abstract fun migrate(root: JsonObject): JsonObject
    
    companion object {
        private val migrators = mutableListOf<ConfigMigrator>()
        fun register(migrator: ConfigMigrator) {
            migrators.add(migrator)
            migrators.sortBy { it.fromVersion }
        }

        fun migrate(root: JsonObject, fromVersion: Int): JsonObject {
            var current = root
            var version = fromVersion
            while (true) {
                val migrator = migrators.firstOrNull { it.fromVersion == version } ?: break
                current = migrator.migrate(current)
                version = migrator.toVersion
            }
            return current
        }
    }
}