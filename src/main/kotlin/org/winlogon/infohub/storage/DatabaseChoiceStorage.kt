package org.winlogon.infohub.storage

import net.kyori.adventure.util.TriState
import org.winlogon.infohub.ChoiceStorage
import org.winlogon.infohub.DatabaseMetadata
import org.winlogon.infohub.DatabaseType
import java.sql.*
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import javax.sql.DataSource

class DatabaseChoiceStorage(private val config: DatabaseMetadata) : ChoiceStorage {
    private lateinit var dataSource: DataSource
    private val taskQueue = ConcurrentLinkedQueue<Pair<UUID, Boolean>>()

    override fun init() {
        val jdbcUrl = when (config.type) {
            DatabaseType.MYSQL -> "jdbc:mysql://${config.host}:${config.port}/${config.database}"
            DatabaseType.POSTGRESQL -> "jdbc:postgresql://${config.host}:${config.port}/${config.database}"
        }
        
        dataSource = when (config.type) {
            DatabaseType.MYSQL -> {
                Class.forName("com.mysql.cj.jdbc.Driver")
                com.zaxxer.hikari.HikariDataSource().apply {
                    setJdbcUrl(jdbcUrl)
                    username = config.username
                    password = config.password
                    maximumPoolSize = 3
                }
            }
            DatabaseType.POSTGRESQL -> {
                Class.forName("org.postgresql.Driver")
                org.postgresql.ds.PGSimpleDataSource().apply {
                    setURL(jdbcUrl)
                    user = config.username
                    password = config.password
                }
            }
        }
        
        createTableIfNotExists()
    }

    private fun createTableIfNotExists() {
        dataSource.connection.use { conn ->
            val sql = """
                CREATE TABLE IF NOT EXISTS ${config.table} (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    enabled BOOLEAN NOT NULL
                )
            """.trimIndent()
            conn.createStatement().execute(sql)
        }
    }

    override fun getChoice(playerUuid: UUID): TriState {
        dataSource.connection.use { conn ->
            val sql = "SELECT enabled FROM ${config.table} WHERE player_uuid = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) {
                        TriState.byBoolean(rs.getBoolean("enabled"))
                    } else {
                        TriState.NOT_SET
                    }
                }
            }
        }
    }

    override fun setChoice(playerUuid: UUID, choice: Boolean) {
        taskQueue.add(playerUuid to choice)
    }

    fun processQueue() {
        while (taskQueue.isNotEmpty()) {
            val (uuid, choice) = taskQueue.poll()
            dataSource.connection.use { conn ->
                val sql = """
                    INSERT INTO ${config.table} (player_uuid, enabled)
                    VALUES (?, ?)
                    ON CONFLICT (player_uuid) 
                    DO UPDATE SET enabled = EXCLUDED.enabled
                """.trimIndent()
                
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, uuid.toString())
                    stmt.setBoolean(2, choice)
                    stmt.executeUpdate()
                }
            }
        }
    }
}
