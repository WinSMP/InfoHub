package org.winlogon.infohub.commands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.executors.CommandExecutor

import org.bukkit.plugin.java.JavaPlugin
import org.winlogon.infohub.utils.PlayerLogger
import org.winlogon.infohub.utils.ServerStats

class UptimeCommand(
    private val playerLogger: PlayerLogger,
    private val startTime: Long
) : PluginCommand {
    override fun register(plugin: JavaPlugin) {
        CommandAPICommand("uptime")
            .withFullDescription("Get the uptime of the Minecraft server.")
            .executes(CommandExecutor { sender, _ ->
                playerLogger.normal(sender, ServerStats.getUptime(startTime, System.nanoTime()))
            })
            .register()
    }
}
