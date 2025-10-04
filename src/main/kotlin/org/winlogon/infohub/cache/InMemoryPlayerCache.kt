package org.winlogon.infohub.cache

import net.kyori.adventure.util.TriState
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * An in-memory implementation of [PlayerCache] using a [ConcurrentHashMap].
 */
class InMemoryPlayerCache : PlayerCache<TriState> {
    private val cache = ConcurrentHashMap<UUID, TriState>()

    override fun get(key: UUID): CompletableFuture<TriState?> = CompletableFuture.completedFuture(cache[key])

    override fun put(key: UUID, value: TriState) {
        cache[key] = value
    }

    override fun evict(key: UUID) {
        cache.remove(key)
    }
}
