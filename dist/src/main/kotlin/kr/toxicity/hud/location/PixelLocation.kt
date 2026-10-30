package kr.toxicity.hud.location

import kr.toxicity.hud.api.yaml.YamlObject
import kr.toxicity.hud.manager.ConfigManagerImpl

data class PixelLocation(
    val x: Int,
    val y: Int,
    val opacity: Double,
    val fractionalX: Double = 0.0,
    val fractionalY: Double = 0.0
) : Comparable<PixelLocation> {
    init {
        require(fractionalX.isFinite() && fractionalX > -1.0 && fractionalX < 1.0) { "Invalid x coordinate fraction: $fractionalX" }
        require(fractionalY.isFinite() && fractionalY > -1.0 && fractionalY < 1.0) { "Invalid y coordinate fraction: $fractionalY" }
    }

    companion object {
        private fun Double.asColor() = coerceAtLeast(0.0).coerceAtMost(1.0)
        private val comparator = Comparator.comparingInt { image: PixelLocation ->
            image.x
        }.thenComparingDouble { image: PixelLocation ->
            image.fractionalX
        }.thenComparingInt { image: PixelLocation ->
            image.y
        }.thenComparingDouble { image: PixelLocation ->
            image.fractionalY
        }.thenComparingDouble { image: PixelLocation ->
            image.opacity
        }

        private val _hotBarHeight = PixelLocation(0, -54, 1.0)
        val hotBarHeight
            get() = if (ConfigManagerImpl.disableLegacyOffset) zero else _hotBarHeight
        val zero = PixelLocation(0, 0, 1.0)
    }
    constructor(section: YamlObject): this(
        section.getAsInt("x", 0),
        section.getAsInt("y", 0),
        section.getAsDouble("opacity", 1.0).asColor(),
        section.getAsDouble("x", 0.0) - section.getAsInt("x", 0),
        section.getAsDouble("y", 0.0) - section.getAsInt("y", 0)
    )
    operator fun plus(other: PixelLocation): PixelLocation {
        val xFraction = fractionalX + other.fractionalX
        val yFraction = fractionalY + other.fractionalY
        val xCarry = xFraction.toInt()
        val yCarry = yFraction.toInt()
        return PixelLocation(
            x + other.x + xCarry,
            y + other.y + yCarry,
            (opacity * other.opacity).asColor(),
            xFraction - xCarry,
            yFraction - yCarry
        )
    }
    override fun compareTo(other: PixelLocation): Int = comparator.compare(this, other)
}
