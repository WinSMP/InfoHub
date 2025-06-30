package org.winlogon.infohub

import net.kyori.adventure.util.TriState
import java.nio.file.Path
import java.util.UUID
import java.time.Duration

interface ChoiceStorage {
    fun init() {}
    fun isConfigOkay(): Boolean = true
    fun getChoice(playerUuid: UUID): TriState
    fun setChoice(playerUuid: UUID, choice: Boolean)
}

sealed class DataSource {
    data class Database(
        val config: DatabaseMetadata,
        val backupInterval: Duration? = null
    ) : DataSource()
    
    data class World(val pdcConfig: PdcConfig) : DataSource()
}

data class DatabaseMetadata(
    val type: DatabaseType,
    val host: String,
    val port: Int,
    val database: String,
    val username: String,
    val password: String,
    val table: String = "infohub_choices"
)

data class PdcConfig(
    val backupInterval: Duration?,
    val usesDatabase: Boolean
)

enum class DatabaseType {
    POSTGRESQL,
    MYSQL
}
