package org.winlogon.infohub

import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.util.TriState

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.winlogon.asynccraftr.AsyncCraftr
import org.winlogon.infohub.cache.*
import org.winlogon.infohub.commands.*
import org.winlogon.infohub.config.*
import org.winlogon.infohub.storage.*
import org.winlogon.infohub.utils.HintHandler
import org.winlogon.infohub.utils.PlayerLogger
import org.winlogon.infohub.utils.ServerStats

import java.time.Duration
import java.util.UUID
import kotlin.random.Random

class InfoHubPlugin : JavaPlugin() {
    private lateinit var mainConfig: MainConfig
    private lateinit var hintHandler: HintHandler
    private lateinit var preferenceRepository: HintPreferenceRepository

    private val miniMessage = MiniMessage.miniMessage()
    private val playerLogger = PlayerLogger()
    private val emojiList = listOf("💡", "📝", "🔍", "📌", "💬", "📖", "🎯")
    private val random = Random.Default
    private var startTime: Long = 0

    override fun onLoad() {
        startTime = System.nanoTime()
        Permissions.register(server.pluginManager)
    }

    override fun onEnable() {
        loadConfiguration()
        hintHandler = HintHandler(
            miniMessage,
            HintConfig(mainConfig.hintList, emojiList),
            Random.Default
        )
        setupStorage()
        registerCommands()
        startSendingHints()
    }

    override fun onDisable() {
        preferenceRepository.close()
    }

    private fun setupStorage() {
        val storageConfig = mainConfig.storage

        val persistentRepository = when (storageConfig.database.type) {
            DatabaseType.SQLITE -> SqliteHintPreferenceRepository(dataFolder)
            else -> JdbcHintPreferenceRepository(storageConfig.database)
        }.also { it.init() }

        val cache: PlayerCache<TriState> = if (storageConfig.redis.enabled) {
            val redisManager = RedisManager(storageConfig.redis.uri)
            RedisPlayerCache(redisManager.connection.async())
        } else {
            InMemoryPlayerCache()
        }

        preferenceRepository = CachedHintPreferenceRepository(persistentRepository, cache)
    }

    private fun startSendingHints() {
        val minutes = random.nextInt(10, 25).toLong()

        AsyncCraftr.runAsyncTaskLater(this, {
            // Snapshot online players to avoid concurrent modification
            val onlinePlayers = server.onlinePlayers.toList()

            // No players online means there is no region thread we can safely anchor to.
            // Reschedule instead of attempting delivery so hints only fire when players exist.
            if (onlinePlayers.isEmpty()) {
                startSendingHints()
                return@runAsyncTaskLater
            }

            // Pick a random player to act as a region anchor
            val randomPlayer = onlinePlayers[random.nextInt(onlinePlayers.size)]

            // Switch to the owning region thread for spatial/player interaction
            AsyncCraftr.runRegionTask(this, randomPlayer.location) {
                val playersInRegion =
                    randomPlayer.world.getNearbyPlayers(randomPlayer.location, 100.0)

                playersInRegion.forEach { player ->
                    // Preference lookup can be async and off-region
                    preferenceRepository
                        .getHintPreference(player.uniqueId)
                        .thenAccept { triState ->
                            if (triState != TriState.FALSE) {
                                // Re-enter the owning region thread before touching the player
                                AsyncCraftr.runRegionTask(this, player.location) {
                                    hintHandler.sendRandomHint(player, emptyList())
                                }
                            }
                        }
                }
            }

            // Schedule the next hint cycle
            startSendingHints()
        }, Duration.ofMinutes(minutes))
    }

    private fun loadConfiguration() {
        saveDefaultConfig()
        mainConfig = loadMainConfig(config)
    }

    private fun registerCommands() {
        val commands = arrayOf(
            PingCommand(mainConfig, playerLogger), RulesCommand(mainConfig, playerLogger),
            DiscordCommand(mainConfig, playerLogger), HelpCommand(mainConfig, playerLogger),
            HintCommand(preferenceRepository, playerLogger)
        )

        for (command in commands) {
            command.register(this)
        }
    }
}
