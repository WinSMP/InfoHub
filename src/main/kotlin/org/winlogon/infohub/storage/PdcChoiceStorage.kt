package org.winlogon.infohub.storage

import net.kyori.adventure.util.TriState

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.OfflinePlayer
import org.bukkit.persistence.PersistentDataType
import org.winlogon.infohub.ChoiceStorage
import org.winlogon.infohub.InfoHubPlugin

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PlayerInformationCache {
    private val playerCache = ConcurrentHashMap<UUID, OfflinePlayer>()

    fun put(uuid: UUID, player: OfflinePlayer) {
        playerCache.put(uuid, player)
    }

    fun get(uuid: UUID): OfflinePlayer? {
        return playerCache[uuid]
    }

    fun evict(uuid: UUID) {
        playerCache.remove(uuid)
    }
}

class PdcChoiceStorage(
    private val plugin: InfoHubPlugin,
    redisUri: String
) : ChoiceStorage {
    private val key = NamespacedKey(plugin, "${plugin.name.lowercase()}-hint-enabled")
    private val logger = plugin.logger
    private val redisManager = RedisManager(redisUri)
    private val cache = RedisPlayerCache(redisManager.conn.sync())

    override fun getChoice(playerUuid: UUID): TriState {
        // get the name from cache
        val name = cache.get(playerUuid)

        // get the player from server cache (the player must have joined at this point) - see setChoice
        val player = if (name != null) Bukkit.getOfflinePlayerIfCached(name)!!
        else Bukkit.getOfflinePlayer(playerUuid)

        cache.put(playerUuid, player.uniqueId.toString())

        val container = player.persistentDataContainer
        return if (container.has(key, PersistentDataType.BOOLEAN)) { 
            // get whether the player has the hint enabled
            container.get(key, PersistentDataType.BOOLEAN)
            ?.let { TriState.byBoolean(it) } ?: TriState.NOT_SET
        } else TriState.NOT_SET
    }

    override fun setChoice(playerUuid: UUID, choice: Boolean) {
        Bukkit.getPlayer(playerUuid)?.let { player ->
            player.persistentDataContainer.set(key, PersistentDataType.BOOLEAN, choice)
            cache.put(playerUuid, player.uniqueId.toString())
        } ?: run {
            logger.warning("Failed to set choice for offline $playerUuid")
        }
    }
}

