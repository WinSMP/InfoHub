package org.winlogon.infohub

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.CommandAPIConfig
import dev.jorel.commandapi.arguments.EntitySelectorArgument
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.util.TriState

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

import org.winlogon.asynccraftr.AsyncCraftr
import org.winlogon.infohub.cache.*
import org.winlogon.infohub.commands.*
import org.winlogon.infohub.config.*
import org.winlogon.infohub.storage.*
import org.winlogon.infohub.utils.*

import java.time.Duration
import java.util.*

import kotlin.random.Random

class InfoHubPlugin : JavaPlugin() {
    private lateinit var mainConfig: MainConfig
    private lateinit var hintHandler: HintHandler
    private lateinit var preferenceRepository: HintPreferenceRepository

    private val miniMessage = MiniMessage.miniMessage()
    private val playerLogger = PlayerLogger()
    private val emojiList = listOf("💡", "📝", "🔍", "📌", "💬", "📖", "🎯")
    private val random = Random.Default
    private val tableRegex = Regex("[a-zA-Z0-9_]+")
    private var startTime: Long = 0

    override fun onLoad() {
        startTime = System.nanoTime()
        // CommandAPI.onLoad(CommandAPIConfig())
    }

    override fun onEnable() {
        // CommandAPI.onEnable()
        mainConfig = loadConfig()
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
        // CommandAPI.onDisable()
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

        storageConfig.backupInterval?.let { interval ->
            if (persistentRepository is JdbcHintPreferenceRepository) {
                AsyncCraftr.runAsyncTaskTimer(this, { persistentRepository.processUpdateQueue() }, interval, interval)
            }
        }
    }

    private fun startSendingHints() {
        val minutes = random.nextInt(10, 25).toLong()
        AsyncCraftr.runAsyncTaskLater(this, {
            val onlinePlayers = server.onlinePlayers
            val playersToHint = onlinePlayers.filter { player ->
                preferenceRepository.getHintPreference(player.uniqueId).get() != TriState.FALSE
            }
            if (playersToHint.isNotEmpty()) {
                hintHandler.sendRandomHint(playersToHint, emptyList())
            }
            startSendingHints()
        }, Duration.ofMinutes(minutes))
    }

    private fun loadConfig(): MainConfig {
        saveDefaultConfig()
        val bukkitConfig = config

        val storageSection = bukkitConfig.getConfigurationSection("storage")!!
        val databaseSection = storageSection.getConfigurationSection("database")!!
        val redisSection = storageSection.getConfigurationSection("redis")!!

        val databaseConfig = DatabaseConfig(
            type = DatabaseType.valueOf(databaseSection.getString("type", "SQLITE")!!.uppercase()),
            host = databaseSection.getString("host", "localhost")!!,
            port = databaseSection.getInt("port", 3306),
            database = databaseSection.getString("database", "minecraft")!!,
            username = databaseSection.getString("username", "root")!!,
            password = databaseSection.getString("password", "")!!,
            table = databaseSection.getString("table", "hint_preferences")!!.takeIf { it.matches(tableRegex) } ?: "hint_preferences"
        )

        val redisConfig = RedisConfig(
            enabled = redisSection.getBoolean("enabled", false),
            uri = redisSection.getString("uri", "redis://localhost:6379")!!
        )

        val storageConfig = StorageConfig(
            database = databaseConfig,
            redis = redisConfig,
            backupInterval = storageSection.getString("backupInterval")?.let { Duration.parse("PT${it.uppercase()}") }
        )

        return MainConfig(
            discordLink = bukkitConfig.getString("discord-link", "https://discord.gg/yourserver")!!,
            rules = bukkitConfig.getStringList("rules"),
            helpMessage = bukkitConfig.getString("help-message", "Use /discord, /rules, or /help for more information!")!!,
            warnUserAboutPing = bukkitConfig.getBoolean("warn-user-ping", false),
            hintList = bukkitConfig.getStringList("hint-list"),
            storage = storageConfig
        )
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
