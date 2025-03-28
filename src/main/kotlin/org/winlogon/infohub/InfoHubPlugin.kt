package org.winlogon.infohub

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.PlayerArgument
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.winlogon.infohub.utils.ServerStats

class InfoHubPlugin : JavaPlugin() {
    private var discordLink: String = "https://discord.gg/yourserver"
    private lateinit var rules: List<String>
    private var helpMessage: String = "Use /discord, /rules, or /help for more information!"
    private var warnUserAboutPing: Boolean = false
    private lateinit var config: Config
    private var startTime: Long = 0
    private val miniMessage = MiniMessage.miniMessage()

    override fun onLoad() {
        startTime = System.nanoTime()
    }

    override fun onEnable() {
        saveDefaultConfig()
        reloadConfig()
        config = loadConfig()
        
        // Register commands
        registerCommands()
        
        logger.info("InfoHub has been enabled!")
    }

    override fun onDisable() {
        CommandAPI.onDisable()
        logger.info("InfoHub has been disabled!")
    }

    private fun loadConfig(): Config {
        logger.info("Loading configuration...")
        val bukkitConfig = getConfig()
        return Config(
            discordLink = bukkitConfig.getString("discord-link") ?: discordLink,
            rules = bukkitConfig.getStringList("rules").takeIf { it.isNotEmpty() } ?: emptyList(),
            helpMessage = bukkitConfig.getString("help-message") ?: helpMessage,
            warnUserAboutPing = bukkitConfig.getBoolean("warn-user-ping", warnUserAboutPing)
        )
    }

    private fun registerCommands() {
        // Discord command
        CommandAPICommand("discord")
            .executes(CommandExecutor { sender, _ ->
                sendFormattedMessage(sender, "<gray>Join our Discord: <dark_aqua>${config.discordLink}")
            })
            .register()

        // Specs command (with alias)
        CommandAPICommand("specs")
            .executes(CommandExecutor { sender, _ ->
                val specs = ServerStats.getSystemSpecs()
                sendFormattedMessage(sender, "§3Server §2Specs")
                sendFormattedMessage(sender, "<gray>- OS: <dark_aqua>${specs.operatingSystem}")
                sendFormattedMessage(sender, "<gray>- Processor: <dark_aqua>${specs.processor}")
                sendFormattedMessage(sender, "<gray>- Physical Cores: <dark_aqua>${specs.physicalCores}")
                sendFormattedMessage(sender, "<gray>- Logical Cores: <dark_aqua>${specs.logicalCores}")
                sendFormattedMessage(sender, "<gray>- Total Memory: <dark_aqua>${specs.totalMemory} GB")
                sendFormattedMessage(sender, "<gray>- Available Memory: <dark_aqua>${specs.availableMemory} GB")
            })
            .register()

        // Rules command (with alias)
        CommandAPICommand("rules")
            .withAliases("rulebook")
            .executes(CommandExecutor { sender, _ ->
                if (config.rules.isEmpty()) {
                    sender.sendMessage("§cError§7: Rules are not configured!")
                    return@CommandExecutor
                }
                sender.sendMessage("<dark_aqua>Server Rules")
                config.rules.forEach { rule -> sender.sendMessage("§7- $rule") }
            })
            .register()

        // Ping command
        CommandTree("ping")
            .then(PlayerArgument("player").replaceSuggestions(ArgumentSuggestions.strings { _ -> 
                Bukkit.getOnlinePlayers().map { it.name }.toTypedArray()
            }))
            .executes(CommandExecutor { sender, args ->
                val target = args["player"] as? Player
                if (target == null) {
                    sender.sendMessage("§cError§7: Player not found.")
                    return@CommandExecutor
                }
                
                val ping = target.ping
                if (ping == 0) {
                    sendFormattedMessage(sender, "<gold>Warning<gray>: The server is unable to determine <dark_aqua>${target.name}<gray>'s ping.")
                    sendFormattedMessage(sender, "<gray>This may be due to the server taking a while to ping them")
                }
                sendFormattedMessage(sender, "<dark_aqua>${target.name}<gray>'s ping is <dark_aqua>$ping ms.")
            })
            .executesPlayer(PlayerCommandExecutor { player, _ ->
                sendFormattedMessage(sender, "<gray>Your ping is <dark_aqua>${player.ping} ms.")
            })
            .register()

        // Uptime command
        CommandAPICommand("uptime")
            .executes(CommandExecutor { sender, _ ->
                ServerStats.getUptime(startTime, System.nanoTime(), sender)
            })
            .register()

        // Help command
        CommandAPICommand("help")
            .executes(CommandExecutor { sender, _ ->
               sendFormattedMessage(sender, "<gray>${showColors(config.helpMessage)}")
            })
            .register()
    }

    fun sendFormattedMessage(sender: CommandSender, message: String) {
        val component = miniMessage.deserialize(message)
        sender.sendMessage(component)
    }
}

data class Config(
    val discordLink: String,
    val rules: List<String>,
    val helpMessage: String,
    val warnUserAboutPing: Boolean
)
