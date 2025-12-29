package org.winlogon.infohub.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

import org.bukkit.command.CommandSender

public class PlayerLogger {
    private val prefix = "<dark_gray>[<level><dark_gray>]<gray> "
    private enum class LogLevel {
        NORMAL,
        DEBUG,
        INFO,
        WARNING,
        ERROR
    }

    private val logColors: Map<LogLevel, TextColor> = mapOf(
        LogLevel.NORMAL  to TextColor.fromHexString("#3590B2")!!,
        LogLevel.DEBUG   to TextColor.fromHexString("#3590B2")!!,
        LogLevel.INFO    to TextColor.fromHexString("#4ABE77")!!,
        LogLevel.WARNING to NamedTextColor.GOLD,
        LogLevel.ERROR   to TextColor.fromHexString("#C32F37")!!,
    )

    private fun log(player: CommandSender, logLevel: LogLevel, message: String, vararg placeholders: Placeholder) {
        val level = Component.text(logLevel.name, logColors[logLevel])
        player.sendRichMessage("<gray>$message", Placeholder.component("level", level), *(placeholders.map { it as TagResolver }).toTypedArray())
    }

    fun normal(player: CommandSender, message: String, vararg placeholders: Placeholder) {
        log(player, LogLevel.NORMAL, message, *placeholders)
    }

    fun normal(player: CommandSender, messages: List<String>) {
        messages.forEach { normal(player, it) }
    }

    fun debug(player: CommandSender, message: String, vararg placeholders: Placeholder) {
        log(player, LogLevel.DEBUG, message, *placeholders)
    }

    fun info(player: CommandSender, message: String, vararg placeholders: Placeholder) {
        log(player, LogLevel.INFO, message, *placeholders)
    }

    fun warning(player: CommandSender, message: String, vararg placeholders: Placeholder) {
        log(player, LogLevel.WARNING, message, *placeholders)
    }

    fun error(player: CommandSender, message: String, vararg placeholders: Placeholder) {
        log(player, LogLevel.ERROR, message, *placeholders)
    }
}
