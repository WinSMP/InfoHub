package org.winlogon.infohub.cache

import io.lettuce.core.api.async.RedisAsyncCommands
import net.kyori.adventure.util.TriState
import java.util.UUID
import java.util.concurrent.CompletableFuture

class RedisPlayerCache(private val redis: RedisAsyncCommands<String, String>) : PlayerCache<TriState> {

    private fun TriState.asString(): String = when (this) {
        TriState.TRUE -> "true"
        TriState.FALSE -> "false"
        TriState.NOT_SET -> "not_set"
    }

    private fun String.toTriState(): TriState = when (this) {
        "true" -> TriState.TRUE
        "false" -> TriState.FALSE
        else -> TriState.NOT_SET
    }

    override fun get(key: UUID): CompletableFuture<TriState?> {
        return redis.get("player-choice:$key").thenApply { it?.toTriState() }.toCompletableFuture()
    }

    override fun put(key: UUID, value: TriState) {
        redis.set("player-choice:$key", value.asString())
    }

    override fun evict(key: UUID) {
        redis.del("player-choice:$key")
    }
}
