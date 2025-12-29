package org.winlogon.infohub.commands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.executors.CommandExecutor

import org.bukkit.plugin.java.JavaPlugin
import org.winlogon.infohub.Permissions
import org.winlogon.infohub.config.MainConfig
import org.winlogon.infohub.utils.PlayerLogger

class RulesCommand(
    private val mainConfig: MainConfig,
    private val playerLogger: PlayerLogger
) : PluginCommand {
    override fun register(plugin: JavaPlugin) {
        CommandAPICommand("rules")
            .withPermission(Permissions.RULES)
            .withAliases("rulebook")
            .executes(CommandExecutor { sender, _ ->
                if (mainConfig.rules.isEmpty()) {
                    playerLogger.error(sender, "Rules aren't configured. Please contact the server admin or staff.")
                } else {
                    playerLogger.normal(sender, "<dark_aqua>Server Rules")
                    mainConfig.rules.forEach { rule -> playerLogger.normal(sender, "- $rule") }
                }
            })
            .register()
    }
}
