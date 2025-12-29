package org.winlogon.infohub.cache

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection

class RedisManager(uri: String) {
    private val client: RedisClient = RedisClient.create(uri)
    val connection: StatefulRedisConnection<String, String> = client.connect()

    fun close() {
        connection.close()
        client.shutdown()
    }
}
