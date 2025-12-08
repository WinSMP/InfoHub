package org.winlogon.infohub.commands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.EntitySelectorArgument
import dev.jorel.commandapi.executors.CommandExecutor

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.winlogon.infohub.config.MainConfig
import org.winlogon.infohub.utils.PlayerLogger

class PingCommand(
    private val mainConfig: MainConfig,
    private val playerLogger: PlayerLogger
) : PluginCommand {
    override fun register(plugin: JavaPlugin) {
        CommandAPICommand("ping")
            .withArguments(EntitySelectorArgument.OnePlayer("player").setOptional(true))
            .executes(CommandExecutor { sender, args ->
                val target = (args["player"] as? Player) ?: sender as? Player
                if (target == null) {
                    playerLogger.error(sender, "You must specify a player when using this command from console.")
                } else {
                    val ping = target.ping
                    if (ping == 0 && mainConfig.warnUserAboutPing) {
                        playerLogger.warning(sender, """
                            The server is unable to determine <dark_aqua>${target.name}<gray>'s ping.
                            This may be due to the server taking a while to ping them.
                        """.trimIndent())
                    }
                    playerLogger.normal(sender, "<dark_aqua>${target.name}<gray>'s ping is <dark_aqua>$ping ms.")
                }
            })
            .register()
    }
}
