package org.winlogon.infohub

import net.kyori.adventure.util.TriState

import java.nio.file.Path
import java.util.UUID

interface ChoiceStorage {
    // TODO: use my own Result library to check for specific errors?
    fun init()
    // TODO: should this return a nullable error enum?
    fun checkConfig(): Boolean
    fun getChoice(playerUuid: UUID): TriState
    // TODO: should this return an error?
    fun setChoice(playerUuid: UUID, choice: Boolean)
}

sealed class DataSource {
    data class Database(val config: DbConfig) : DataSource()
    data class SQLite(val path: Path) : DataSource()
    data class Local(val path: Path) : DataSource()
}

/** Non-embedded RDBMS configuration */
data class DbConfig(
    // Should handle the URL type accordingly
    val type: DatabaseType,
    val username: String,
    val password: String,
    val maxConnections: Int
)

enum class DatabaseType {
    POSTGRESQL,
    MYSQL,
}
