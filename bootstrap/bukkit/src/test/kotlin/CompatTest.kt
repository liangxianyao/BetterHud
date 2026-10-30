import kr.toxicity.hud.api.placeholder.HudPlaceholder
import kr.toxicity.hud.bootstrap.bukkit.compatibility.Compatibility
import kr.toxicity.hud.bootstrap.bukkit.compatibility.mythicmobs.FirstFloorBossHudMath
import kr.toxicity.hud.bootstrap.bukkit.compatibility.mythicmobs.FirstFloorBossHudSnapshot
import kr.toxicity.hud.bootstrap.bukkit.manager.CompatibilityManager
import kr.toxicity.hud.bootstrap.bukkit.module.MODULE_BUKKIT
import kr.toxicity.hud.bootstrap.bukkit.module.Module
import kr.toxicity.hud.manager.ListenerManagerImpl
import kr.toxicity.hud.manager.PlaceholderManagerImpl
import kr.toxicity.hud.manager.TriggerManagerImpl
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import java.io.OutputStream
import java.io.PrintStream
import kotlin.io.path.Path
import kotlin.io.path.outputStream

class CompatTest {

    private fun placeholderMapper(n: String? = null) = { entry: Map.Entry<String, HudPlaceholder<*>> ->
        val length = entry.value.requiredArgsLength
        buildString {
            append(if (n != null) "**${n}_${entry.key}**" else "**${entry.key}**")
            if (length > 0) {
                append(':')
                append(if (length == 1) "arg" else (0..<length).joinToString(",") { i ->
                    "args${i + 1}"
                })
            }
        }
    }

    private fun <T> Collection<T>.printForEach(name: String, stringMapper: (T) -> String = {
        if (it is String) it else it.toString()
    }) {
        if (isNotEmpty()) {
            println("### $name")
            forEach {
                println(" - ${stringMapper(it)}")
            }
        }
    }

    private lateinit var stream: PrintStream
    private lateinit var output: OutputStream

    @BeforeEach
    fun setup() {
        stream = System.out
        output = Path(System.getProperty("user.dir").substringBefore("bootstrap"))
            .resolve("build")
            .resolve("placeholders-bukkit.md")
            .outputStream()
            .buffered().apply {
                System.setOut(PrintStream(this))
            }
    }
    @AfterEach
    fun end() {
        System.setOut(stream)
        output.close()
    }

    @Test
    fun compatTest() {
        val mapper = placeholderMapper()

        println("# Default")
        TriggerManagerImpl.allTriggerKeys.printForEach("**Triggers**")
        ListenerManagerImpl.allListenerKeys.printForEach("**Listeners**")
        PlaceholderManagerImpl.stringContainer.allPlaceholders.entries.printForEach("**Placeholders** (String)", mapper)
        PlaceholderManagerImpl.numberContainer.allPlaceholders.entries.printForEach("**Placeholders** (Number)", mapper)
        PlaceholderManagerImpl.booleanContainer.allPlaceholders.entries.printForEach("**Placeholders** (Boolean)", mapper)
        println("")

        println("# Bukkit")
        MODULE_BUKKIT.forEach {
            it.value().print(it.key)
        }
        println("")

        println("# Compatibility")
        CompatibilityManager.compatibilities.forEach {
            it.value().print(it.key)
        }
    }

    @Test
    fun firstFloorBossHudRatios() {
        assertEquals("huangjie_1c1boss", FirstFloorBossHudMath.templateSlug("HUANGJIE_1C1BOSS"))
        assertEquals("huangjie_1c2boss", FirstFloorBossHudMath.templateSlug("huangjie_1C2BOSS"))
        assertNull(FirstFloorBossHudMath.templateSlug("zhanggu_2c1BOSS"))

        val legacyName = "§6荒界统帅·§f赫尔萨"
        val convertedName = FirstFloorBossHudMath.legacyToMiniMessage(legacyName)
        assertEquals(LegacyComponentSerializer.legacySection().deserialize(legacyName),
            MiniMessage.miniMessage().deserialize(convertedName))
        val legacyHexName = "§x§F§F§0§0§0§0Boss§r"
        assertEquals(Component.text("Boss", TextColor.color(0xFF0000)),
            MiniMessage.miniMessage().deserialize(FirstFloorBossHudMath.legacyToMiniMessage(legacyHexName)))

        val boss = FirstFloorBossHudSnapshot("huangjie_1c1boss", "Boss", 450.0, 1000.0)
        assertEquals(1.0, FirstFloorBossHudMath.segmentRatio(boss, 1))
        assertEquals(0.5, FirstFloorBossHudMath.segmentRatio(boss, 5))
        assertEquals(0.0, FirstFloorBossHudMath.segmentRatio(boss, 6))
        assertEquals(0.0, FirstFloorBossHudMath.segmentRatio(boss, 0))
        assertEquals(0.0, FirstFloorBossHudMath.segmentRatio(null, 1))
        assertEquals(0.0, FirstFloorBossHudMath.segmentRatio(boss.copy(maxHealth = 0.0), 1))
        assertEquals(0.0, FirstFloorBossHudMath.segmentRatio(boss.copy(health = Double.NaN), 1))
    }

    private fun Module.print(name: String) {
        val mapper = if (name == "standard") placeholderMapper() else placeholderMapper(name)

        println("## $name")
        triggers.keys.printForEach("**Triggers**")
        listeners.keys.printForEach("**Listeners**")
        strings.entries.printForEach("**Placeholders** (String)", mapper)
        numbers.entries.printForEach("**Placeholders** (Number)", mapper)
        booleans.entries.printForEach("**Placeholders** (Boolean)", mapper)
        println("")
    }

    private fun Compatibility.print(name: String) {
        val n = name.lowercase().replace('-', '_')

        val mapper = placeholderMapper(n)

        println("## [$name]($website)")
        triggers.keys.printForEach("**Triggers**")
        listeners.keys.printForEach("**Listeners**")
        strings.entries.printForEach("**Placeholders** (String)", mapper)
        numbers.entries.printForEach("**Placeholders** (Number)", mapper)
        booleans.entries.printForEach("**Placeholders** (Boolean)", mapper)
        println("")
    }
}
