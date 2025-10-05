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
                    preference BOOLEAN DEFAULT NULL
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
                when (choice) {
                    TriState.TRUE -> stmt.setBoolean(2, true)
                    TriState.FALSE -> stmt.setBoolean(2, false)
                    TriState.NOT_SET -> stmt.setNull(2, java.sql.Types.BOOLEAN)
                }
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