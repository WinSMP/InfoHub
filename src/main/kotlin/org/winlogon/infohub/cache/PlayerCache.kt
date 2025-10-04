package org.winlogon.infohub.cache

import net.kyori.adventure.util.TriState
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * A generic interface for caching player data.
 *
 * @param V The type of data to be cached.
 */
interface PlayerCache<V> {
    /**
     * Retrieves a value from the cache.
     *
     * @param key The UUID of the player.
     * @return A [CompletableFuture] that will complete with the cached value, or null if not found.
     */
    fun get(key: UUID): CompletableFuture<V?>

    /**
     * Puts a value into the cache.
     *
     * @param key The UUID of the player.
     * @param value The value to be cached.
     */
    fun put(key: UUID, value: V)

    /**
     * Evicts a value from the cache.
     *
     * @param key The UUID of the player.
     */
    fun evict(key: UUID)

    /**
     * Closes the cache and releases any resources.
     */
    fun close() {}
}
