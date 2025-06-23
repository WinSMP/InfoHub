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
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.winlogon.infohub.utils.HintHandler
import org.winlogon.infohub.utils.ServerStats
import java.util.function.Consumer

class InfoHubPlugin : JavaPlugin() {
    private lateinit var rules: List<String>
    private lateinit var config: Config
    private lateinit var hintHandler: HintHandler

    private var discordLink: String = "https://discord.gg/yourserver"
    private var helpMessage: String = "Use /discord, /rules, or /help for more information!"
    private var warnUserAboutPing: Boolean = false

    private val hintList: MutableList<String> = mutableListOf()
    private val ignoredPlayers: MutableList<Player> = mutableListOf()

    private var startTime: Long = 0
    private var isFolia: Boolean = false

    private val miniMessage = MiniMessage.miniMessage()
    private val playerLogger = PlayerLogger()
    private val emojiList: List<String> = listOf("💡", "📝", "🔍", "📌", "💬", "📖", "🎯")
    private val random = java.security.SecureRandom()

    override fun onLoad() {
        startTime = System.nanoTime()
    }

    override fun onEnable() {
        saveDefaultConfig()
        reloadConfig()
        config = loadConfig()
        isFolia = try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
            true
        } catch(e: ClassNotFoundException) {
            false
        }

        val hintConfig = HintConfig(
            hintList = config.hintList,
            iconEmojis = emojiList,
        )
        hintHandler = HintHandler(miniMessage, hintConfig, random)
        
        registerCommands()

        logger.info("Starting background hint sender")
        startSendingHints()
    }

    override fun onDisable() {
    }

    private fun startSendingHints() {
        val baseDur = (60 * 1000)
        // from 5 to 20 minutes as Minecraft ticks
        val randDur = random.nextInt(5 * baseDur, 20 * baseDur) / 50
        val randomTime = randDur.toLong()

        val task = Runnable {
            // run the hint task again after running it
            hintHandler.sendRandomHint(Bukkit.getOnlinePlayers().toList(), ignoredPlayers)
            startSendingHints()
        }

        if (isFolia) {
            val scheduler = Bukkit.getServer().getGlobalRegionScheduler()
            // needed because the task is repeated
            scheduler.runDelayed(this, Consumer { _ ->
                task.run()
            }, randomTime)
        } else {
            Bukkit.getScheduler().runTaskLaterAsynchronously(this, task, randomTime)
        }
    }

    private fun loadConfig(): Config {
        logger.info("Loading configuration...")
        val bukkitConfig = getConfig()
        return Config(
            discordLink = bukkitConfig.getString("discord-link") ?: discordLink,
            rules = bukkitConfig.getStringList("rules").takeIf { it.isNotEmpty() } ?: emptyList(),
            helpMessage = bukkitConfig.getString("help-message") ?: helpMessage,
            warnUserAboutPing = bukkitConfig.getBoolean("warn-user-ping", warnUserAboutPing),
            hintList = bukkitConfig.getStringList("hint-list").takeIf { it.isNotEmpty() } ?: emptyList()
        )
    }

    private fun registerCommands() {
        // Discord command
        CommandAPICommand("discord")
            .executes(CommandExecutor { sender, _ ->
                playerLogger.normal(sender, "Join our Discord: <dark_aqua>${config.discordLink}")
            })
            .register()

        // Specs command
        CommandAPICommand("specs")
            .executes(CommandExecutor { sender, _ ->
                val specs = ServerStats.getSystemSpecs()
                playerLogger.normal(sender, "<dark_aqua>Server Specs")
                playerLogger.normal(sender, "- OS: <dark_aqua>${specs.operatingSystem}")
                playerLogger.normal(sender, "- Processor: <dark_aqua>${specs.processor}")
                playerLogger.normal(sender, "- Physical Cores: <dark_aqua>${specs.physicalCores}")
                playerLogger.normal(sender, "- Logical Cores: <dark_aqua>${specs.logicalCores}")
                playerLogger.normal(sender, "- Total Memory: <dark_aqua>${specs.totalMemory} GB")
                playerLogger.normal(sender, "- Available Memory: <dark_aqua>${specs.availableMemory} GB")
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
                        if (ignoredPlayers.contains(sender)) {
                            playerLogger.normal(sender, "Hints are already <dark_aqua>disabled<gray> for you.")
                            return@PlayerCommandExecutor
                        }
                        ignoredPlayers.add(sender)
                        playerLogger.normal(sender, "Got it! Hints are now <dark_aqua>disabled<gray> for you.")
                    }),
                CommandAPICommand("enable")
                    .executesPlayer(PlayerCommandExecutor { sender, _ ->
                        if (!ignoredPlayers.contains(sender)) {
                            playerLogger.normal(sender, "Hints are already <dark_aqua>enabled<gray> for you.")
                            return@PlayerCommandExecutor
                        }
                        ignoredPlayers.remove(sender)
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
)

data class HintConfig(
    val hintList: List<String>,
    val iconEmojis: List<String>,
)
