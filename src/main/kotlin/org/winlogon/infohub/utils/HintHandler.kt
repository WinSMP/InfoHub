package org.winlogon.infohub.utils;

import java.util.Random

class Color(val r: Int, val g: Int, val b: Int) {
	fun toRgb(): String {
		return String.format("#%02x%02x%02x", r, g, b)
	}

	fun toHsl(): String {
		return String.format("hsl(%d, %d%%, %d%%)", r, g, b)
	}
}

class HintHandler {
	private var random = Random()

	init {
		
	}

	fun getRandomColor(): Color {
		val red = random.nextInt(256)
		val green = random.nextInt(256)
		val blue = random.nextInt(256)
		return Color(red, green, blue)
	}



}
