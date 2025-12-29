package org.winlogon.infohub.commands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.BooleanArgument
import dev.jorel.commandapi.arguments.EntitySelectorArgument
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor

import net.kyori.adventure.util.TriState

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.winlogon.infohub.Permissions
import org.winlogon.infohub.storage.HintPreferenceRepository
import org.winlogon.infohub.utils.PlayerLogger

class HintCommand(
    private val preferenceRepository: HintPreferenceRepository,
    private val playerLogger: PlayerLogger
) : PluginCommand {
    override fun register(plugin: JavaPlugin) {
        CommandAPICommand("hint")
            .withFullDescription("Enable or disable hints about the server.")
            .withSubcommands(
                CommandAPICommand("disable")
                    .executesPlayer(PlayerCommandExecutor { sender, _ ->
                        preferenceRepository.setHintPreference(sender.uniqueId, TriState.FALSE).thenRun {
                            playerLogger.normal(sender, "Got it! Hints are now <dark_aqua>disabled<gray> for you.")
                        }
                    }),
                CommandAPICommand("enable")
                    .executesPlayer(PlayerCommandExecutor { sender, _ ->
                        preferenceRepository.setHintPreference(sender.uniqueId, TriState.TRUE).thenRun {
                            playerLogger.normal(sender, "Okay, hints are <dark_aqua>enabled<gray> now.")
                        }
                    }),
                CommandAPICommand("toggle")
                    .withArguments(EntitySelectorArgument.OnePlayer("player"), BooleanArgument("enabled"))
                    .withPermission(Permissions.HINT_TOGGLE)
                    .executes(CommandExecutor { sender, args ->
                        val player = args["player"] as Player
                        val enabled = args["enabled"] as Boolean
                        val choice = if (enabled) TriState.TRUE else TriState.FALSE
                        preferenceRepository.setHintPreference(player.uniqueId, choice).thenRun {
                            playerLogger.normal(
                                sender,
                                "Hints for <dark_aqua>${player.name}<gray> are now <dark_aqua>${if (enabled) "enabled" else "disabled"}<gray>."
                            )
                        }
                    })
            )
            .register()
    }
}
