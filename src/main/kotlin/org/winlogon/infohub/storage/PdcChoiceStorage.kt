package org.winlogon.infohub.storage

import net.kyori.adventure.util.TriState

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.OfflinePlayer
import org.bukkit.persistence.PersistentDataType
import org.winlogon.infohub.ChoiceStorage
import org.winlogon.infohub.InfoHubPlugin

import java.util.UUID

class PdcChoiceStorage(private val plugin: InfoHubPlugin) : ChoiceStorage {
    private val key = NamespacedKey(plugin, "${plugin.name.lowercase()}-hint-enabled")
    private val logger = plugin.logger

    override fun getChoice(playerUuid: UUID): TriState {
        val player = Bukkit.getOfflinePlayer(playerUuid)
        val container = player.persistentDataContainer
        
        return if (container.has(key, PersistentDataType.BOOLEAN)) {
            val value = container.get(key, PersistentDataType.BOOLEAN) ?: return TriState.NOT_SET
            TriState.byBoolean(value)
        } else {
            TriState.NOT_SET
        }
    }

    override fun setChoice(playerUuid: UUID, choice: Boolean) {
        val player = Bukkit.getPlayer(playerUuid) ?: run {
            logger.warning("Failed to set PDC choice for offline player $playerUuid")
            return
        }
        player.persistentDataContainer.set(key, PersistentDataType.BOOLEAN, choice)
    }
}
