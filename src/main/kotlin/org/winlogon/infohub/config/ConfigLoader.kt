package org.winlogon.infohub.config

import org.bukkit.configuration.file.FileConfiguration
import org.winlogon.infohub.util.tableRegex
import java.time.Duration

fun loadMainConfig(bukkitConfig: FileConfiguration): MainConfig {
    val storageConfig = loadStorageConfig(bukkitConfig)
    return MainConfig(
        discordLink = bukkitConfig.getString("discord-link", "https://discord.gg/yourserver")!!,
        rules = bukkitConfig.getStringList("rules"),
        helpMessage = bukkitConfig.getString("help-message", "Use /discord, /rules, or /help for more information!")!!,
        warnUserAboutPing = bukkitConfig.getBoolean("warn-user-ping", false),
        hintList = bukkitConfig.getStringList("hint-list"),
        storage = storageConfig
    )
}

private fun loadStorageConfig(bukkitConfig: FileConfiguration): StorageConfig {
    val storageSection = bukkitConfig.getConfigurationSection("storage")!!
    val databaseConfig = loadDatabaseConfig(storageSection)
    val redisConfig = loadRedisConfig(storageSection)
    return StorageConfig(
        database = databaseConfig,
        redis = redisConfig,
        backupInterval = storageSection.getString("backupInterval")?.let { Duration.parse("PT${it.uppercase()}") }
    )
}

private fun loadDatabaseConfig(storageSection: org.bukkit.configuration.ConfigurationSection): DatabaseConfig {
    val databaseSection = storageSection.getConfigurationSection("database")!!
    return DatabaseConfig(
        type = DatabaseType.valueOf(databaseSection.getString("type", "SQLITE")!!.uppercase()),
        host = databaseSection.getString("host", "localhost")!!,
        port = databaseSection.getInt("port", 3306),
        database = databaseSection.getString("database", "minecraft")!!,
        username = databaseSection.getString("username", "root")!!,
        password = databaseSection.getString("password", "")!!,
        table = databaseSection.getString("table", "hint_preferences")!!.takeIf { it.matches(tableRegex) } ?: "hint_preferences"
    )
}

private fun loadRedisConfig(storageSection: org.bukkit.configuration.ConfigurationSection): RedisConfig {
    val redisSection = storageSection.getConfigurationSection("redis")!!
    return RedisConfig(
        enabled = redisSection.getBoolean("enabled", false),
        uri = redisSection.getString("uri", "redis://localhost:6379")!!
    )
}
