package org.winlogon

import org.bukkit.command.{Command, CommandSender, CommandExecutor}
import org.bukkit.{Bukkit, ChatColor}
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

import org.winlogon.utils.ServerStats

case class Config(
  discordLink: String,
  rules: List[String],
  helpMessage: String,
  warnUserAboutPing: Boolean
)

class InfoHubCommand(private val mainClass: InfoHub, config: Config) extends CommandExecutor {
  override def onCommand(
      sender: CommandSender,
      command: Command,
      label: String,
      args: Array[String]
  ): Boolean = {
    command.getName.toLowerCase match {
      case "discord" =>
        sender.sendMessage(s"§7Join our Discord: §3${config.discordLink}")
        true
      case "specs" | "whatdoesthisserveruse" =>
        val specs = ServerStats.getSystemSpecs()
        sender.sendMessage(s"§3Server §2Specs")
        sender.sendMessage(s"§7- OS: §3${specs.operatingSystem}")
        sender.sendMessage(s"§7- Processor: §3${specs.processor}")
        sender.sendMessage(s"§7- Physical Cores: §3${specs.physicalCores}")
        sender.sendMessage(s"§7- Logical Cores: §3${specs.logicalCores}")
        sender.sendMessage(s"§7- Total Memory: §3${specs.totalMemory} GB")
        sender.sendMessage(s"§7- Available Memory: §3${specs.availableMemory} GB")
        true
      case "rules" | "rulebook" =>
        if (config.rules.isEmpty) {
          sender.sendMessage(s"§cError§7: Rules are not configured!")
          return false
        }
        sender.sendMessage(s"§3Server §2Rules")
        config.rules.foreach(rule => sender.sendMessage(s"§7- $rule"))
        true
      case "ping" =>
        handlePingCommand(sender, args, config)
        true
      case "uptime" =>
        ServerStats.getUptime(mainClass.startTime, System.nanoTime, sender)
        true
      case "help" =>
        sender.sendMessage(s"§7${showColors(config.helpMessage)}")
        true
      case _ =>
        false
    }
  }

  private def handlePingCommand(sender: CommandSender, args: Array[String], config: Config): Boolean = {
    // No arguments, check the sender's ping
    if (args.isEmpty) {
      sender match {
        case player: Player =>
          val ping = player.getPing
          player.sendMessage(s"§7Your ping is §3$ping ms§7.")
        case _ =>
          sender.sendMessage("§cError§7: Only players can see their own ping.")
      }
    } else {
      // Check another player's ping
      val targetPlayer = Option(Bukkit.getServer.getPlayer(args(0)))

      targetPlayer match {
        case Some(player) =>
          val ping = player.getPing
          if (ping == 0) {
            sender.sendMessage(s"§6Warning§7: The server is unable to determine §3${player.getName}§7's ping.")
            sender.sendMessage("§7This may be due to the server taking a while to pinging them")
          }
          sender.sendMessage(s"§3${player.getName}§7's ping is §3$ping ms§7.")
        case None =>
          sender.sendMessage(s"§cError§7: Player §3${args(0)}§7 not found.")
      }
    }
    false
  }

  def showColors(s: String): String = ChatColor.translateAlternateColorCodes('&', s)
}
