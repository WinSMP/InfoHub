package org.winlogon.infohub.config

import java.time.Duration

data class StorageConfig(
    val database: DatabaseConfig,
    val redis: RedisConfig,
    val backupInterval: Duration?
)

data class DatabaseConfig(
    val type: DatabaseType,
    val host: String,
    val port: Int,
    val database: String,
    val username: String,
    val password: String,
    val table: String = "hint_preferences",
    val properties: Map<String, String> = emptyMap()
)

data class RedisConfig(
    val enabled: Boolean,
    val uri: String
)

enum class DatabaseType {
    MYSQL,
    POSTGRESQL,
    SQLITE
}
