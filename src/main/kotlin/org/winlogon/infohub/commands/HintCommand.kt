package org.winlogon.infohub.commands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.executors.PlayerCommandExecutor

import net.kyori.adventure.util.TriState

import org.bukkit.plugin.java.JavaPlugin
import org.winlogon.infohub.storage.HintPreferenceRepository
import org.winlogon.infohub.utils.PlayerLogger

class HintCommand(
    private val preferenceRepository: HintPreferenceRepository,
    private val playerLogger: PlayerLogger
) : PluginCommand {
    override fun register(plugin: JavaPlugin) {
        CommandAPICommand("hint")
            .withSubcommands(
                CommandAPICommand("disable")
                    .executesPlayer(PlayerCommandExecutor { sender, _ ->
                        preferenceRepository.setHintPreference(sender.uniqueId, TriState.FALSE)
                        playerLogger.normal(sender, "Got it! Hints are now <dark_aqua>disabled<gray> for you.")
                    }),
                CommandAPICommand("enable")
                    .executesPlayer(PlayerCommandExecutor { sender, _ ->
                        preferenceRepository.setHintPreference(sender.uniqueId, TriState.TRUE)
                        playerLogger.normal(sender, "Okay, hints are <dark_aqua>enabled<gray> now.")
                    })
            )
            .register()
    }
}
