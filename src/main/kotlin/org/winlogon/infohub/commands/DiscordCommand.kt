package org.winlogon.infohub.commands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.executors.CommandExecutor

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder

import org.bukkit.plugin.java.JavaPlugin
import org.winlogon.infohub.config.MainConfig
import org.winlogon.infohub.utils.PlayerLogger

class DiscordCommand(
    private val mainConfig: MainConfig,
    private val playerLogger: PlayerLogger
) : PluginCommand {
    override fun register(plugin: JavaPlugin) {
        CommandAPICommand("discord")
            .executes(CommandExecutor { sender, _ ->
                val clickableDiscordInvite = Component.text(mainConfig.discordLink)
                    .color(NamedTextColor.DARK_AQUA)
                    .clickEvent(ClickEvent.openUrl(mainConfig.discordLink))

                sender.sendRichMessage(
                    "<gray>Join our Discord: <discord-link>",
                    Placeholder.component("discord-link", clickableDiscordInvite)
                )
            })
            .register()

    }
}
