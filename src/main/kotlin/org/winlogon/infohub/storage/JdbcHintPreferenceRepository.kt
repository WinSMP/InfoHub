package org.winlogon.infohub.storage

import com.zaxxer.hikari.HikariDataSource

import net.kyori.adventure.util.TriState

import org.winlogon.infohub.config.DatabaseConfig
import org.winlogon.infohub.config.DatabaseType

import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * A [HintPreferenceRepository] that uses a database to store player hint preferences.
 */
class JdbcHintPreferenceRepository(private val config: DatabaseConfig) : HintPreferenceRepository {
    private lateinit var dataSource: HikariDataSource
    private val updateQueue = ConcurrentLinkedQueue<Pair<UUID, TriState>>()

    override fun init() {
        when (config.type) {
            DatabaseType.MYSQL -> Class.forName("com.mysql.cj.jdbc.Driver")
            DatabaseType.POSTGRESQL -> Class.forName("org.postgresql.Driver")
            DatabaseType.SQLITE -> throw IllegalArgumentException("SQLite should be handled by SqliteHintPreferenceRepository")
        }

        dataSource = HikariDataSource().apply {
            jdbcUrl = when (config.type) {
                DatabaseType.MYSQL -> "jdbc:mysql://${config.host}:${config.port}/${config.database}"
                DatabaseType.POSTGRESQL -> "jdbc:postgresql://${config.host}:${config.port}/${config.database}"
                DatabaseType.SQLITE -> throw IllegalArgumentException("SQLite should be handled by SqliteHintPreferenceRepository")
            }
            username = config.username
            password = config.password
            maximumPoolSize = 3
            config.properties.forEach { (key, value) -> addDataSourceProperty(key, value) }
        }
        createTableIfNotExists()
    }

    private fun createTableIfNotExists() {
        dataSource.connection.use { connection ->
            val sql = """
                CREATE TABLE IF NOT EXISTS ${config.table} (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    preference BOOLEAN DEFAULT NULL
                )
            """.trimIndent()
            connection.createStatement().execute(sql)
        }
    }

    override fun getHintPreference(playerUuid: UUID): CompletableFuture<TriState> {
        return CompletableFuture.supplyAsync {
            dataSource.connection.use { connection ->
                val sql = "SELECT preference FROM ${config.table} WHERE player_uuid = ?"
                connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, playerUuid.toString())
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            val preference = rs.getObject("preference") as? Boolean
                            when (preference) {
                                true -> TriState.TRUE
                                false -> TriState.FALSE
                                null -> TriState.NOT_SET
                            }
                        } else {
                            TriState.NOT_SET
                        }
                    }
                }
            }
        }
    }

    override fun setHintPreference(playerUuid: UUID, choice: TriState) {
        updateQueue.add(playerUuid to choice)
    }

    fun processUpdateQueue(): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            val updates = generateSequence { updateQueue.poll() }.toList()
            if (updates.isEmpty()) return@runAsync

            dataSource.connection.use { connection ->
                val sql = when (config.type) {
                    DatabaseType.MYSQL -> """
                        INSERT INTO ${config.table} (player_uuid, preference) VALUES (?, ?)
                        ON DUPLICATE KEY UPDATE preference = VALUES(preference)
                    """
                    DatabaseType.POSTGRESQL -> """
                        INSERT INTO ${config.table} (player_uuid, preference) VALUES (?, ?)
                        ON CONFLICT (player_uuid) DO UPDATE SET preference = EXCLUDED.preference
                    """
                    else -> return@use
                }
                connection.prepareStatement(sql).use { stmt ->
                    for ((uuid, choice) in updates) {
                        stmt.setString(1, uuid.toString())
                        when (choice) {
                            TriState.TRUE -> stmt.setBoolean(2, true)
                            TriState.FALSE -> stmt.setBoolean(2, false)
                            TriState.NOT_SET -> stmt.setNull(2, java.sql.Types.BOOLEAN)
                        }
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
            }
        }
    }

    override fun close() {
        if (::dataSource.isInitialized) {
            processUpdateQueue().join() // process remaining queue before closing
            dataSource.close()
        }
    }
}
