package org.winlogon

import org.bukkit.plugin.java.JavaPlugin

class InfoHub extends JavaPlugin {
  private val commands = List("discord", "rules", "rulebook", "ping", "help")
  private var discordLink: String = "https://discord.gg/yourserver"
  private var rules: List[String] = List.empty
  private var helpMessage: String = "Use /discord, /rules, or /help for more information!"
  private var warnUserAboutPing: Boolean = false
  private var config: Config = Config(discordLink, rules, helpMessage, warnUserAboutPing)

  override def onEnable(): Unit = {
    saveDefaultConfig()
    config = loadConfig()
    val infoHubExecutor = new InfoHubCommand(config)

    // Register commands
    // getCommand("discord").setExecutor(infoHubExecutor)
    // getCommand("rules").setExecutor(infoHubExecutor)
    // getCommand("rulebook").setExecutor(infoHubExecutor)
    // getCommand("ping").setExecutor(infoHubExecutor)
    // getCommand("help").setExecutor(infoHubExecutor)
    for (command <- commands) {
      getCommand(command).setExecutor(infoHubExecutor)
    }

    getLogger.info("InfoHub has been enabled!")
  }

  override def onDisable(): Unit = {
    getLogger.info("InfoHub has been disabled!")
  }

  private def loadConfig(): Config = {
    Config(
      discordLink = getConfig.getString("discord-link"),
      rules = getConfig.getStringList("rules").toArray.map(_.toString).toList,
      helpMessage = getConfig.getString("help-message", helpMessage),
      warnUserAboutPing = getConfig.getBoolean("warn-user-ping", warnUserAboutPing)
    )
  }
}
