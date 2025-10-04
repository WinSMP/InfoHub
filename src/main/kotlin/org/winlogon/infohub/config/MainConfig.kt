package org.winlogon.infohub.config

import java.time.Duration

data class MainConfig(
    val discordLink: String,
    val rules: List<String>,
    val helpMessage: String,
    val warnUserAboutPing: Boolean,
    val hintList: List<String>,
    val storage: StorageConfig
)

data class HintConfig(
    val hintList: List<String>,
    val iconEmojis: List<String>,
)
