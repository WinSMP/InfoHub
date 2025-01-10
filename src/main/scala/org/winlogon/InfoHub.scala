package org.winlogon

import org.bukkit.plugin.java.JavaPlugin

class InfoHub extends JavaPlugin {
  private val commands = List(
    "discord", "dc", "whatisthediscord", "rules", "rulebook",
    "ping", "help", "specs", "whatdoesthisserveruse", "uptime",
  )
  private var discordLink: String = "https://discord.gg/yourserver"
  private var rules: List[String] = List.empty
  private var helpMessage: String = "Use /discord, /rules, or /help for more information!"
  private var warnUserAboutPing: Boolean = false
  private var config: Config = Config(discordLink, rules, helpMessage, warnUserAboutPing)
  var startTime: Long = _

  val logger = this.getLogger

  override def onEnable(): Unit = {
    saveDefaultConfig()
    config = loadConfig()
    val infoHubExecutor = InfoHubCommand(this, config)

    // Register commands
    for (command <- commands) {
      getCommand(command).setExecutor(infoHubExecutor)
    }

    startTime = System.nanoTime
    logger.info("InfoHub has been enabled!")
  }

  override def onDisable(): Unit = {
    logger.info("InfoHub has been disabled!")
  }

  private def loadConfig(): Config = {
    logger.info("Loading configuration...")
    Config(
      discordLink = getConfig.getString("discord-link"),
      rules = getConfig.getStringList("rules").toArray.map(_.toString).toList,
      helpMessage = getConfig.getString("help-message", helpMessage),
      warnUserAboutPing = getConfig.getBoolean("warn-user-ping", warnUserAboutPing)
    )
  }
}
