package org.winlogon.infohub.utils

import net.kyori.adventure.text.minimessage.MiniMessage

import org.bukkit.entity.Player
import org.winlogon.infohub.config.HintConfig

import kotlin.random.Random

class HintHandler(
    private val miniMessage: MiniMessage,
    private val hintConfig: HintConfig,
	private val random: Random
) {

    private fun getRandomColor(): Pair<String, String> {
        val minBrightness = 75
        val maxBrightness = 85

        val brighterColor = Color.getRandom(null, minBrightness, maxBrightness, random)
        val (hue, _, _) = brighterColor.toHSL()

        val hueVariation = random.nextInt(-5, 5 + 1)
        val newHue = (hue + hueVariation + 360) % 360

        val darkerMin = (minBrightness - 15).coerceAtLeast(0)
        val darkerMax = (maxBrightness - 15).coerceAtMost(100)
        val darkerColor = Color.getRandom(newHue, darkerMin, darkerMax, random)

        return darkerColor.toHex() to brighterColor.toHex()
    }

    private fun getRandomHint(): String {
        val list = hintConfig.hintList
        return list.random(random)
    }

    private fun getRandomHintEmoji(): String {
        val iconEmojis = hintConfig.iconEmojis
        return iconEmojis.random(random)
    }

    private fun getRandomHintWithColor(): String {
        val (start, end) = getRandomColor()
        val hint = getRandomHint()
        val icon = getRandomHintEmoji()
        return "<$start>[$icon]</$start> <gradient:$start:$end>$hint</gradient>"
    }

    fun sendRandomHint(player: Player, ignoredPlayers: List<Player>) {
        if (!player.isOnline || player in ignoredPlayers) {
            return
        }

        val hint = getRandomHintWithColor()
        val formattedMsg = miniMessage.deserialize(hint)
        player.sendMessage(formattedMsg)
    }
}
