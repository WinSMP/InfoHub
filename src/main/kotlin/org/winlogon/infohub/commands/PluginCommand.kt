package org.winlogon.infohub.commands

import org.bukkit.plugin.java.JavaPlugin

interface PluginCommand {
    fun register(plugin: JavaPlugin)
}