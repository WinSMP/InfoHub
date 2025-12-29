package org.winlogon.infohub.commands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.executors.CommandExecutor

import org.bukkit.plugin.java.JavaPlugin
import org.winlogon.infohub.utils.PlayerLogger
import org.winlogon.infohub.utils.ServerStats

class SpecsCommand(
    private val playerLogger: PlayerLogger
) : PluginCommand {
    override fun register(plugin: JavaPlugin) {
        CommandAPICommand("specs")
            .withFullDescription("Get the hardware and software specs of the server, from OS to CPU.")
            .executes(CommandExecutor { sender, _ ->
                val specs = ServerStats.getSystemSpecs()
                playerLogger.normal(
                    sender,
                    """
                    <dark_aqua>Server Specs
                    - OS: <dark_aqua>${specs.operatingSystem}
                    - Processor: <dark_aqua>${specs.processor}
                    - Physical Cores: <dark_aqua>${specs.physicalCores}
                    - Logical Cores: <dark_aqua>${specs.logicalCores}
                    - Total Memory: <dark_aqua>${specs.totalMemory} GB
                    - Available Memory: <dark_aqua>${specs.availableMemory} GB
                    """.trimIndent().lines()
                )
            })
            .register()
    }
}
