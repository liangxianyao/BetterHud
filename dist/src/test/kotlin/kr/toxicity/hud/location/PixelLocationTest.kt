package kr.toxicity.hud.location

import kr.toxicity.hud.shader.HudShader
import kr.toxicity.hud.shader.RenderScale
import kr.toxicity.hud.yaml.YamlObjectImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class PixelLocationTest {
    private fun location(x: Number, y: Number = 0): PixelLocation =
        PixelLocation(YamlObjectImpl("", mutableMapOf("x" to x, "y" to y)))

    @Test
    fun fractionalCoordinatesRemainDistinctFromWholePixels() {
        assertNotEquals(PixelLocation(1, 0, 1.0), location(1.5, -0.5))
        assertNotEquals(PixelLocation(0, 0, 1.0), location(0.5))
        assertNotEquals(PixelLocation(0, 0, 1.0), location(0, -0.5))
        assertEquals(PixelLocation(1, 0, 1.0), location(1))
    }

    @Test
    fun addingLocationsKeepsTheFractionalOffset() {
        val combined = location(1.5) + location(-0.25)
        assertNotEquals(PixelLocation(1, 0, 1.0), combined)
        assertEquals(location(1.25), combined)
    }

    @Test
    fun shadersWithDifferentFractionalOffsetsHaveDifferentKeys() {
        fun shader(x: Number): HudShader {
            val offset = location(x)
            return HudShader(
                GuiLocation(0.0, 0.0),
                RenderScale.fromConfig(offset, YamlObjectImpl.empty),
                0, 0, 1.0, 0
            )
        }
        val whole = shader(1)
        val half = shader(1.5)
        assertNotEquals(whole, half)
        assertNotEquals(0, whole.compareTo(half))

        val raised = HudShader(
            GuiLocation(0.0, 0.0),
            RenderScale.fromConfig(location(1, -0.5), YamlObjectImpl.empty),
            0, 0, 1.0, 0
        )
        assertNotEquals(whole, raised)
        assertNotEquals(0, whole.compareTo(raised))
    }

    @Test
    fun nonFiniteCoordinatesAreRejected() {
        assertFailsWith<IllegalArgumentException> { location(Double.NaN) }
        assertFailsWith<IllegalArgumentException> { location(Double.POSITIVE_INFINITY) }
    }
}
