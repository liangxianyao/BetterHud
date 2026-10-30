package kr.toxicity.hud.shader

import kr.toxicity.hud.location.GuiLocation

data class HudShader(
    val gui: GuiLocation,
    val renderScale: RenderScale,
    val layer: Int,
    val outline: Int,
    val opacity: Double,
    val property: Int,
    val fractionalX: Double = renderScale.relativeOffset.fractionalX,
    val fractionalY: Double = renderScale.relativeOffset.fractionalY,
) : Comparable<HudShader> {
    companion object {
        private val comparator = Comparator.comparing { s: HudShader ->
            s.gui
        }.thenComparing { s: HudShader ->
            s.renderScale
        }.thenComparing { s: HudShader ->
            s.layer
        }.thenComparing { s: HudShader ->
            s.outline
        }.thenComparing { s: HudShader ->
            s.opacity
        }.thenComparing { s: HudShader ->
            s.property
        }.thenComparingDouble { s: HudShader ->
            s.fractionalX
        }.thenComparingDouble { s: HudShader ->
            s.fractionalY
        }
    }

    override fun compareTo(other: HudShader): Int {
        return comparator.compare(this, other)
    }

    fun toBackground(otherOpacity: Double) = HudShader(
        gui,
        renderScale,
        layer,
        0,
        opacity * otherOpacity,
        property,
        fractionalX,
        fractionalY
    )

    fun toFancyHead() = HudShader(
        gui,
        renderScale * 1.125,
        layer + 1,
        0x8F shl 24,
        opacity,
        property,
        fractionalX,
        fractionalY
    )
}
