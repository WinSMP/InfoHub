package org.winlogon.infohub.commands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.executors.CommandExecutor

import org.bukkit.plugin.java.JavaPlugin
import org.winlogon.infohub.config.MainConfig
import org.winlogon.infohub.utils.PlayerLogger

class HelpCommand(
    private val mainConfig: MainConfig,
    private val playerLogger: PlayerLogger
) : PluginCommand {
    override fun register(plugin: JavaPlugin) {
        CommandAPICommand("helpme")
            .executes(CommandExecutor { sender, _ ->
                playerLogger.normal(sender, mainConfig.helpMessage)
            })
            .register()
    }
}
