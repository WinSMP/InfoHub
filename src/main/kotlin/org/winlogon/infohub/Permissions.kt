package org.winlogon.infohub

import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.bukkit.plugin.PluginManager

object Permissions {
    const val PING = "infohub.ping"
    const val RULES = "infohub.rules"
    const val DISCORD = "infohub.discord"
    const val HELP = "infohub.help"
    const val HINT_TOGGLE = "infohub.hint.toggle"

    fun register(pluginManager: PluginManager) {
        pluginManager.addPermission(Permission(PING, "Allows the user to use the /ping command", PermissionDefault.NOT_OP))
        pluginManager.addPermission(Permission(RULES, "Allows the user to use the /rules command", PermissionDefault.NOT_OP))
        pluginManager.addPermission(Permission(DISCORD, "Allows the user to use the /discord command", PermissionDefault.NOT_OP))
        pluginManager.addPermission(Permission(HELP, "Allows the user to use the /help command", PermissionDefault.NOT_OP))
        pluginManager.addPermission(Permission(HINT_TOGGLE, "Allows the user to toggle hints for other players", PermissionDefault.OP))
    }
}
