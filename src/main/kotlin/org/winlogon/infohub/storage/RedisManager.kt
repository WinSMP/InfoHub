package org.winlogon.infohub.storage

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection

class RedisManager(uri: String) {
    private val client = RedisClient.create(uri)
    val conn: StatefulRedisConnection<String, String> = client.connect()
}
