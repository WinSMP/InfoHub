package org.winlogon.infohub.storage

import net.kyori.adventure.util.TriState
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID
import java.util.concurrent.CompletableFuture

class SqliteHintPreferenceRepository(private val dataFolder: File) : HintPreferenceRepository {
    private lateinit var connection: Connection
    private val tableName = "hint_preferences"

    override fun init() {
        val dbFile = dataFolder.resolve("infohub.db").also { it.parentFile.mkdirs(); it.createNewFile() }
        connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
        createTableIfNotExists()
    }

    private fun createTableIfNotExists() {
        connection.createStatement().use { stmt ->
            val sql = """
                CREATE TABLE IF NOT EXISTS $tableName (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    preference VARCHAR(10) NOT NULL
                )
            """
            stmt.execute(sql)
        }
    }

    override fun getHintPreference(playerUuid: UUID): CompletableFuture<TriState> {
        return CompletableFuture.supplyAsync {
            val sql = "SELECT preference FROM $tableName WHERE player_uuid = ?"
            connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        TriState.valueOf(rs.getString("preference").uppercase())
                    } else {
                        TriState.NOT_SET
                    }
                }
            }
        }
    }

    override fun setHintPreference(playerUuid: UUID, choice: TriState) {
        CompletableFuture.runAsync {
            val sql = """
            INSERT INTO $tableName (player_uuid, preference)
            VALUES (?, ?)
            ON CONFLICT (player_uuid)
            DO UPDATE SET preference = EXCLUDED.preference
        """
            connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, choice.name.lowercase())
                stmt.executeUpdate()
            }
        }
    }

    override fun close() {
        if (::connection.isInitialized && !connection.isClosed) {
            connection.close()
        }
    }
}