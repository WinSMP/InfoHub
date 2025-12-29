package org.winlogon.infohub.utils

import kotlin.random.Random

class Color(val r: Int, val g: Int, val b: Int, private val random: Random) {
    fun toHex(): String {
        return String.format("#%02x%02x%02x", r, g, b)
    }

    fun toHSL(): Triple<Int, Int, Int> {
        val rNorm = r / 255.0
        val gNorm = g / 255.0
        val bNorm = b / 255.0

        val max = maxOf(rNorm, gNorm, bNorm)
        val min = minOf(rNorm, gNorm, bNorm)
        val delta = max - min

        val h: Int
        val l = ((max + min) / 2 * 100).toInt()

        // Calculate hue
        h = when {
            delta == 0.0 -> 0
            max == rNorm -> ((60 * ((gNorm - bNorm) / delta) + 360) % 360).toInt()
            max == gNorm -> ((60 * ((bNorm - rNorm) / delta) + 120) % 360).toInt()
            else -> ((60 * ((rNorm - gNorm) / delta) + 240) % 360).toInt()
        }

        // Calculate saturation
        val s = if (delta == 0.0) 0 else (delta / (1 - Math.abs(2 * l / 100 - 1) + 0.0001) * 100).toInt()

        return Triple(h, s, l)
    }

    companion object {
        fun getRandom(maxHue: Int?, minBrightness: Int, maxBrightness: Int, random: Random): Color {
            val inputHue = maxHue ?: 360
            val hue = random.nextInt(inputHue + 1)
            val saturation = random.nextInt(60, 90 + 1)
            val lightness = random.nextInt(maxBrightness - minBrightness + 1) + minBrightness

            return hslToRgb(hue, saturation, lightness, random)
        }

        private fun hslToRgb(h: Int, s: Int, l: Int, random: Random): Color {
            val sNorm = s / 100.0
            val lNorm = l / 100.0

            val c = (1 - Math.abs(2 * lNorm - 1)) * sNorm
            val x = c * (1 - Math.abs((h / 60) % 2 - 1))
            val m = lNorm - c / 2

            val (rPrime, gPrime, bPrime) = when {
                h < 60 -> Triple(c, x, 0.0)
                h < 120 -> Triple(x, c, 0.0)
                h < 180 -> Triple(0.0, c, x)
                h < 240 -> Triple(0.0, x, c)
                h < 300 -> Triple(x, 0.0, c)
                else -> Triple(c, 0.0, x)
            }

            val r = ((rPrime + m) * 255).toInt()
            val g = ((gPrime + m) * 255).toInt()
            val b = ((bPrime + m) * 255).toInt()

            return Color(r, g, b, random)
        }
    }
}
