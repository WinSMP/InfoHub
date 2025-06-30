package org.winlogon.infohub.storage

import io.lettuce.core.api.sync.RedisCommands
import java.util.UUID

class RedisPlayerCache(redisCommands: RedisCommands<String, String>) {
    private val redis = redisCommands

    fun put(uuid: UUID, name: String) {
        redis.set("player:$uuid", name)
    }

    fun get(uuid: UUID): String? {
        return redis.get("player:$uuid")
    }

    fun evict(uuid: UUID) {
        redis.del("player:$uuid")
    }
}
