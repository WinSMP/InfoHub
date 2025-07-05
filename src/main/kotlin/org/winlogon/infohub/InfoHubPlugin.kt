package org.winlogon.infohub

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.PlayerArgument
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.util.TriState

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.winlogon.asynccraftr.AsyncCraftr
import org.winlogon.infohub.utils.HintHandler
import org.winlogon.infohub.utils.ServerStats

import org.winlogon.infohub.storage.CombinedChoiceStorage
import org.winlogon.infohub.storage.DatabaseChoiceStorage
import org.winlogon.infohub.storage.PdcChoiceStorage

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.function.Consumer
import kotlin.text.uppercase

class InfoHubPlugin : JavaPlugin() {
    private lateinit var rules: List<String>
    private lateinit var config: Config
    private lateinit var hintHandler: HintHandler

    private var discordLink: String = "https://discord.gg/yourserver"
    private var helpMessage: String = "Use /discord, /rules, or /help for more information!"
    private var warnUserAboutPing: Boolean = false

    private val hintList: MutableList<String> = mutableListOf()
    private lateinit var choiceManager: ChoiceManager
    private val ignoredPlayers: MutableSet<UUID> = mutableSetOf()

    private var startTime: Long = 0
    public val isFolia: Boolean = try {
        Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
        true
    } catch(e: ClassNotFoundException) {
        false
    }

    private val miniMessage = MiniMessage.miniMessage()
    private val playerLogger = PlayerLogger()
    private val emojiList: List<String> = listOf("💡", "📝", "🔍", "📌", "💬", "📖", "🎯")
    private val random = java.security.SecureRandom()
    private val tableRegex = Regex("[a-zA-Z0-9_]+")

    override fun onLoad() {
        startTime = System.nanoTime()
    }

    private fun setupChoiceStorage() {
        val storageConfig = config.storageConfig
        val storage: ChoiceStorage = when (storageConfig.mode) {
            "pdc" -> createPdcStorage()
            "database" -> createDatabaseStorage(storageConfig.databaseConfig)
            "both" -> createCombinedStorage(storageConfig)
            else -> throw IllegalArgumentException("Invalid storage mode: ${storageConfig.mode}")
        }
        
        choiceManager = ChoiceManager(storage, this, storageConfig.backupInterval)
        choiceManager.start()
    }

    private fun createPdcStorage(): ChoiceStorage {
        return PdcChoiceStorage(this, config.storageConfig.redisUri)
    }

    private fun createDatabaseStorage(dbConfig: DatabaseMetadata?): ChoiceStorage {
        requireNotNull(dbConfig) { "Database configuration is required for database storage mode" }
        return DatabaseChoiceStorage(dbConfig).apply { init() }
    }

    private fun createCombinedStorage(storageConfig: StorageConfig): ChoiceStorage {
        val pdcStorage = PdcChoiceStorage(this, config.storageConfig.redisUri)
        val dbStorage = createDatabaseStorage(storageConfig.databaseConfig)
        return CombinedChoiceStorage(pdcStorage, dbStorage)
    }

    override fun onEnable() {
        saveDefaultConfig()
        reloadConfig()
        config = loadConfig()

        val hintConfig = HintConfig(
            hintList = config.hintList,
            iconEmojis = emojiList,
        )
        hintHandler = HintHandler(miniMessage, hintConfig, random)
        
        registerCommands()

        logger.info("Starting background hint sender")

        setupChoiceStorage()
        startSendingHints()
    }

    override fun onDisable() {
    }

    private fun startSendingHints() {
        val minutes = random.nextInt(5, 20)
        val delay = Duration.ofMinutes(minutes.toLong())

        AsyncCraftr.runAsyncTaskLater(this, {
            val allPlayers = Bukkit.getOnlinePlayers().toList()
            val ignoredPlayersList = ignoredPlayers.mapNotNull { Bukkit.getPlayer(it) }
            hintHandler.sendRandomHint(allPlayers, ignoredPlayersList)
            startSendingHints()
        }, delay)
    }

    private fun loadConfig(): Config {
        logger.info("Loading configuration...")
        val bukkitConfig = getConfig()
        
        // get configuration storage: and deeper from config.yml
        val storageSection = bukkitConfig.getConfigurationSection("storage")

        val storageConfig = StorageConfig(
            mode = storageSection?.getString("mode") ?: "pdc",
            backupInterval = storageSection?.getString("backupInterval")?.let {
                Duration.parse("PT${it.uppercase()}") 
            },
            databaseConfig = storageSection?.getConfigurationSection("database")?.let { dbSection ->
                val databaseType = dbSection.getString("type") ?: "MYSQL"
                DatabaseMetadata(
                    type = DatabaseType.valueOf(databaseType.uppercase()),
                    host = dbSection.getString("host") ?: "localhost",
                    port = dbSection.getInt("port", 3306),
                    database = dbSection.getString("database") ?: "minecraft",
                    username = dbSection.getString("username") ?: "root",
                    password = dbSection.getString("password") ?: "",
                    // get table name, and if it doesn't look correct, default to infohub_choices
                    table = dbSection.getString("table")?.takeIf { it.matches(tableRegex) } ?: "infohub_choices"
                )
            },
            redisUri = storageSection?.getString("redis-uri") ?: "redis://localhost:6379"
        )

        return Config(
            discordLink = bukkitConfig.getString("discord-link") ?: discordLink,
            rules = bukkitConfig.getStringList("rules").takeIf { it.isNotEmpty() } ?: emptyList(),
            helpMessage = bukkitConfig.getString("help-message") ?: helpMessage,
            warnUserAboutPing = bukkitConfig.getBoolean("warn-user-ping", warnUserAboutPing),
            hintList = bukkitConfig.getStringList("hint-list").takeIf { it.isNotEmpty() } ?: emptyList(),
            storageConfig = storageConfig
        )
    }

    private fun registerCommands() {
        // Discord command
        CommandAPICommand("discord")
            .executes(CommandExecutor { sender, _ ->
                val clickableDiscordInvite = Component.text(config.discordLink)
                    .color(NamedTextColor.DARK_AQUA)
                    .clickEvent(ClickEvent.openUrl(config.discordLink))

                sender.sendRichMessage(
                    "<gray>Join our Discord: <discord-link>",
                    Placeholder.component("discord-link", clickableDiscordInvite)
                )
            })
            .register()

        // Specs command
        CommandAPICommand("specs")
            .executes(CommandExecutor { sender, _ ->
                val specs = ServerStats.getSystemSpecs()
                playerLogger.normal(
                    sender,
                    arrayOf(
                        "<dark_aqua>Server Specs",
                        "- OS: <dark_aqua>${specs.operatingSystem}",
                        "- Processor: <dark_aqua>${specs.processor}",
                        "- Physical Cores: <dark_aqua>${specs.physicalCores}",
                        "- Logical Cores: <dark_aqua>${specs.logicalCores}",
                        "- Total Memory: <dark_aqua>${specs.totalMemory} GB",
                        "- Available Memory: <dark_aqua>${specs.availableMemory} GB"
                    )
                )
            })
            .register()

        CommandAPICommand("rules")
            .withAliases("rulebook")
            .executes(CommandExecutor { sender, _ ->
                if (config.rules.isEmpty()) {
                    playerLogger.error(sender, "Rules aren't configured. Please contact the server admin or staff.")
                    return@CommandExecutor
                }
                playerLogger.normal(sender, "<dark_aqua>Server Rules")
                config.rules.forEach { rule -> playerLogger.normal(sender, "- $rule") }
            })
            .register()

        CommandAPICommand("ping")
            .withArguments(PlayerArgument("player").setOptional(true))
            .executes(CommandExecutor { sender, args ->
                // Try to get the "player" argument; if not provided and sender is a Player, use the sender
                val target = (args["player"] as? Player) ?: if (sender is Player) sender else {
                    playerLogger.error(sender, "You must specify a player when using this command from console.")
                    return@CommandExecutor
                }

                val ping = target.ping

                if (ping == 0 && config.warnUserAboutPing) {
                    playerLogger.warning(sender, """
                        The server is unable to determine <dark_aqua>${target.name}<gray>'s ping.
                        This may be due to the server taking a while to ping them.
                    """.trimIndent())
                }

                playerLogger.normal(sender, "<dark_aqua>${target.name}<gray>'s ping is <dark_aqua>$ping ms.")
            })
            .register()

        CommandAPICommand("uptime")
            .executes(CommandExecutor { sender, _ ->
                val msg = ServerStats.getUptime(startTime, System.nanoTime())
                playerLogger.normal(sender, msg)
            })
            .register()

        CommandAPICommand("helpme")
            .executes(CommandExecutor { sender, _ ->
                playerLogger.normal(sender, "${config.helpMessage}")
            })
            .register()

            CommandAPICommand("hint")
            .withSubcommands(
                CommandAPICommand("disable")
                    .executesPlayer(PlayerCommandExecutor { sender, _ ->
                        choiceManager.setChoice(sender.uniqueId, false)
                        ignoredPlayers.add(sender.uniqueId)
                        playerLogger.normal(sender, "Got it! Hints are now <dark_aqua>disabled<gray> for you.")
                    }),
                CommandAPICommand("enable")
                    .executesPlayer(PlayerCommandExecutor { sender, _ ->
                        choiceManager.setChoice(sender.uniqueId, true)
                        ignoredPlayers.remove(sender.uniqueId)
                        playerLogger.normal(sender, "Okay, hints are <dark_aqua>enabled<gray> now.")
                    })
            )
            .register()
    }
}

class PlayerLogger {
    val miniMessage: MiniMessage
    lateinit var prefix: String

    init {
        miniMessage = MiniMessage.miniMessage()
    }

    fun formatMessage(msg: String): Component {
        return miniMessage.deserialize(msg)
    }

    public fun normal(player: CommandSender, message: String) {
        prefix = "<gray>"
        player.sendMessage(formatMessage(prefix + message))
    }

    public fun normal(player: CommandSender, messages: Array<String>) {
        prefix = "<gray>"
        messages.forEach { player.sendMessage(formatMessage(prefix + it)) }
    }

    public fun debug(player: CommandSender, message: String) {
        prefix = "<dark_gray>[<#3590B2>DEBUG<dark_gray>]<gray> "
        player.sendMessage(formatMessage(prefix + message))
    }

    public fun info(player: CommandSender, message: String) {
        prefix = "<dark_gray>[<#4ABE77>INFO<dark_gray>]<gray> "
        player.sendMessage(formatMessage(prefix + message))
    }

    public fun warning(player: CommandSender, message: String) {
        prefix = "<dark_gray>[<gold>WARNING<dark_gray>]<gray> "
        player.sendMessage(formatMessage(prefix + message))
    }

    public fun error(player: CommandSender, message: String) {
        prefix = "<dark_gray>[<#C32F37>ERROR<dark_gray>]<gray> "
        player.sendMessage(formatMessage(prefix + message))
    }
}


data class Config(
    val discordLink: String,
    val rules: List<String>,
    val helpMessage: String,
    val warnUserAboutPing: Boolean,
    val hintList: List<String>,
    val storageConfig: StorageConfig
)

data class HintConfig(
    val hintList: List<String>,
    val iconEmojis: List<String>,
)


data class StorageConfig(
    val mode: String, // "pdc", "database", or "both"
    val databaseConfig: DatabaseMetadata? = null,
    val backupInterval: Duration? = null,
    val redisUri: String
)

class ChoiceManager(
    private val storage: ChoiceStorage,
    private val plugin: InfoHubPlugin,
    private val backupInterval: Duration?
) {
    private val taskId = -1

    fun getChoice(playerUuid: UUID): TriState {
        return storage.getChoice(playerUuid)
    }

    fun setChoice(playerUuid: UUID, choice: Boolean) {
        storage.setChoice(playerUuid, choice)
    }

    fun start() {
        if (backupInterval != null && storage is DatabaseChoiceStorage) {
            AsyncCraftr.runAsyncTaskTimer(
                plugin,
                { storage.processQueue() },
                backupInterval,
                backupInterval
            )
        }
    }
}
