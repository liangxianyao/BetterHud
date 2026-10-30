package kr.toxicity.hud.bootstrap.bukkit.compatibility.mythicmobs

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.skills.SkillCaster
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent
import io.lumine.mythic.core.mobs.ActiveMob
import io.lumine.mythic.core.players.PlayerData
import io.lumine.mythic.core.skills.AbstractSkill
import kr.toxicity.hud.api.listener.HudListener
import kr.toxicity.hud.api.placeholder.HudPlaceholder
import kr.toxicity.hud.api.player.HudPlayer
import kr.toxicity.hud.api.trigger.HudTrigger
import kr.toxicity.hud.api.update.UpdateEvent
import kr.toxicity.hud.api.yaml.YamlObject
import kr.toxicity.hud.bootstrap.bukkit.compatibility.Compatibility
import kr.toxicity.hud.bootstrap.bukkit.compatibility.mythicmobs.event.MythicMobsPopupEvent
import kr.toxicity.hud.bootstrap.bukkit.compatibility.mythicmobs.mechanic.HidePopupMechanic
import kr.toxicity.hud.bootstrap.bukkit.compatibility.mythicmobs.mechanic.ShowPopupMechanic
import kr.toxicity.hud.bootstrap.bukkit.util.bukkitPlayer
import kr.toxicity.hud.bootstrap.bukkit.util.unwrap
import kr.toxicity.hud.util.BOOTSTRAP
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.Locale
import java.util.UUID
import java.util.function.Function

class MythicMobsCompatibility : Compatibility {

    override val website: String = "https://www.spigotmc.org/resources/5702/"

    override val triggers: Map<String, (YamlObject) -> HudTrigger<*>>
        get() = mapOf()
    override val listeners: Map<String, (YamlObject) -> (UpdateEvent) -> HudListener>
        get() = mapOf()

    private fun <T> HudPlayer.profile(fallback: T, mapper: PlayerData.() -> T) = MythicBukkit.inst().playerManager.getProfile(bukkitPlayer)?.let(mapper) ?: fallback

    override val numbers: Map<String, HudPlaceholder<Number>>
        get() = mapOf(
            "boss_unit" to HudPlaceholder.of<Number> { _, _ -> Function { 1.0 } },
            "boss_health" to HudPlaceholder.of<Number> { _, _ -> Function { p ->
                FirstFloorBossHudTracker.snapshot(p.uuid())?.health ?: 0.0
            } },
            "boss_max_health" to HudPlaceholder.of<Number> { _, _ -> Function { p ->
                FirstFloorBossHudTracker.snapshot(p.uuid())?.maxHealth ?: 0.0
            } },
        ) + (1..10).associate { segment ->
            "boss_segment_$segment" to HudPlaceholder.of<Number> { _, _ -> Function { p ->
                FirstFloorBossHudMath.segmentRatio(FirstFloorBossHudTracker.snapshot(p.uuid()), segment)
            } }
        } + mapOf(
            "current_cooldown" to HudPlaceholder.builder<Number>()
                .requiredArgsLength(1)
                .function { args, _ ->
                    val skill = MythicBukkit.inst().skillManager.getSkill(args[0]).orElseThrow {
                        RuntimeException("this skill doesn't exist: ${args[0]}")
                    } as AbstractSkill
                    Function { p ->
                        skill.getCooldown(object : SkillCaster {
                            override fun getEntity(): AbstractEntity = BukkitAdapter.adapt(p.bukkitPlayer)
                            override fun setUsingDamageSkill(p0: Boolean) {}
                            override fun isUsingDamageSkill(): Boolean = false
                        })
                    }
                }
                .build(),
            "aura_stack" to HudPlaceholder.builder<Number>()
                .requiredArgsLength(1)
                .function { args, _ ->
                    Function { p ->
                        p.profile(0) {
                            getAuraStacks(args[0])
                        }
                    }
                }
                .build(),
            "aura_max_duration" to HudPlaceholder.builder<Number>()
                .requiredArgsLength(1)
                .function { args, _ ->
                    Function { p ->
                        p.profile(0) {
                            auraRegistry.auras[args[0]]?.maxOfOrNull {
                                it.startDuration
                            } ?: 0
                        }
                    }
                }
                .build(),
            "aura_duration" to HudPlaceholder.builder<Number>()
                .requiredArgsLength(1)
                .function { args, _ ->
                    Function { p ->
                        p.profile(0) {
                            auraRegistry.auras[args[0]]?.maxOfOrNull {
                                it.ticksRemaining
                            } ?: 0
                        }
                    }
                }
                .build(),
            "aura_duration_reversed" to HudPlaceholder.builder<Number>()
                .requiredArgsLength(1)
                .function { args, _ ->
                    Function { p ->
                        p.profile(0) {
                            auraRegistry.auras[args[0]]?.maxByOrNull {
                                it.startDuration
                            }?.let {
                                it.startDuration - it.ticksRemaining
                            } ?: 0
                        }
                    }
                }
                .build(),
            // entity
            "entity_current_cooldown" to HudPlaceholder.builder<Number>()
                .requiredArgsLength(1)
                .function { args, reason ->
                    reason.unwrap { event: EntityEvent ->
                        val skill = MythicBukkit.inst().skillManager.getSkill(args[0]).orElseThrow {
                            RuntimeException("this skill doesn't exist: ${args[0]}")
                        } as AbstractSkill
                        Function {
                            skill.getCooldown(object : SkillCaster {
                                override fun getEntity(): AbstractEntity = BukkitAdapter.adapt(event.entity)
                                override fun setUsingDamageSkill(p0: Boolean) {}
                                override fun isUsingDamageSkill(): Boolean = false
                            })
                        }
                    }
                }
                .build(),
            "entity_aura_stack" to HudPlaceholder.builder<Number>()
                .requiredArgsLength(1)
                .function { args, reason ->
                    reason.unwrap { event: EntityEvent ->
                        Function get@ {
                            (MythicBukkit.inst().mobManager.getMythicMobInstance(event.entity) ?: return@get -1).getAuraStacks(args[0])
                        }
                    }
                }
                .build(),
            "entity_aura_max_duration" to HudPlaceholder.builder<Number>()
                .requiredArgsLength(1)
                .function { args, reason ->
                    reason.unwrap { event: EntityEvent ->
                        Function get@ {
                            (MythicBukkit.inst().mobManager.getMythicMobInstance(event.entity) ?: return@get -1).auraRegistry.auras[args[0]]?.maxOfOrNull { aura ->
                                aura.startDuration
                            } ?: 0
                        }
                    }
                }
                .build(),
            "entity_aura_duration" to HudPlaceholder.builder<Number>()
                .requiredArgsLength(1)
                .function { args, reason ->
                    reason.unwrap { event: EntityEvent ->
                        Function get@ {
                            (MythicBukkit.inst().mobManager.getMythicMobInstance(event.entity) ?: return@get -1).auraRegistry.auras[args[0]]?.maxOfOrNull { aura ->
                                aura.ticksRemaining
                            } ?: 0
                        }
                    }
                }
                .build(),
            "entity_aura_duration_reversed" to HudPlaceholder.builder<Number>()
                .requiredArgsLength(1)
                .function { args, reason ->
                    reason.unwrap { event: EntityEvent ->
                        Function get@ {
                            (MythicBukkit.inst().mobManager.getMythicMobInstance(event.entity) ?: return@get -1).auraRegistry.auras[args[0]]?.maxOfOrNull { aura ->
                                aura.startDuration - aura.ticksRemaining
                            } ?: 0
                        }
                    }
                }
                .build()
        )
    override val strings: Map<String, HudPlaceholder<String>>
        get() = mapOf(
            "boss_name" to HudPlaceholder.of { _, _ -> Function { p ->
                FirstFloorBossHudTracker.snapshot(p.uuid())?.name.orEmpty()
            } },
            "boss_template_id" to HudPlaceholder.of { _, _ -> Function { p ->
                FirstFloorBossHudTracker.snapshot(p.uuid())?.templateId.orEmpty()
            } },
            "caster_variable" to HudPlaceholder.builder<String>()
                .requiredArgsLength(1)
                .function { args, reason ->
                    val value = args[0]
                    reason.unwrap { e: MythicMobsPopupEvent ->
                        val registry = when (val caster = e.caster) {
                            is PlayerData -> caster.variables.get(value)
                            is ActiveMob -> MythicBukkit.inst().variableManager.getRegistry(caster).get(value)
                            else -> null
                        }
                        Function {
                            registry?.get()?.toString() ?: "<none>"
                        }
                    }
                }
                .build(),
            "target_variable" to HudPlaceholder.builder<String>()
                .requiredArgsLength(1)
                .function { args, reason ->
                    val value = args[0]
                    reason.unwrap { e: MythicMobsPopupEvent ->
                        val registry = MythicBukkit.inst().playerManager.getProfile(e.target).variables.get(value)
                        Function {
                            registry?.get()?.toString() ?: "<none>"
                        }
                    }
                }
                .build(),
            "world_variable" to HudPlaceholder.builder<String>()
                .requiredArgsLength(1)
                .function { args, _ ->
                    val registry = MythicBukkit.inst().variableManager.globalRegistry.get(args[0])
                    Function {
                        registry?.get()?.toString() ?: "<none>"
                    }
                }
                .build(),
            "entity_id" to HudPlaceholder.builder<String>()
                .function { args, reason ->
                    reason.unwrap { event: EntityEvent ->
                        Function get@ {
                            (MythicBukkit.inst().mobManager.getMythicMobInstance(event.entity) ?: return@get "<none>").mobType
                        }
                    }
                }
                .build(),
        )
    override val booleans: Map<String, HudPlaceholder<Boolean>>
        get() = mapOf(
            "boss_visible" to HudPlaceholder.of { _, _ -> Function { p ->
                FirstFloorBossHudTracker.snapshot(p.uuid()) != null
            } },
            "has_aura" to HudPlaceholder.builder<Boolean>()
                .requiredArgsLength(1)
                .function { args, _ ->
                    Function { p ->
                        p.profile(false) {
                            auraRegistry.hasAura(args[0])
                        }
                    }
                }
                .build(),
            "is_mythicmob" to HudPlaceholder.of { _, u ->
                u.unwrap { e: EntityEvent ->
                    Function {
                        MythicBukkit.inst().mobManager.isMythicMob(e.entity)
                    }
                }
            },
            "entity_has_aura" to HudPlaceholder.builder<Boolean>()
                .requiredArgsLength(1)
                .function { args, reason ->
                    reason.unwrap { event: EntityEvent ->
                        Function get@ { _ ->
                            (MythicBukkit.inst().mobManager.getMythicMobInstance(event.entity) ?: return@get false).auraRegistry.hasAura(args[0])
                        }
                    }
                }
                .build()
        )

    override fun start() {
        FirstFloorBossHudTracker.start()
        Bukkit.getPluginManager().registerEvents(object : Listener {
            @EventHandler
            fun load(e: MythicMechanicLoadEvent) {
                when (e.mechanicName.lowercase()) {
                    "showpopup" -> e.register(ShowPopupMechanic(e.config))
                    "hidepopup" -> e.register(HidePopupMechanic(e.config))
                }
            }
        }, BOOTSTRAP as Plugin)
    }

    override fun stop() {
        FirstFloorBossHudTracker.stop()
    }
}

internal data class FirstFloorBossHudSnapshot(
    val templateId: String,
    val name: String,
    val health: Double,
    val maxHealth: Double
)

internal object FirstFloorBossHudMath {
    private val legacySerializer = LegacyComponentSerializer.legacySection()
    private val miniMessageSerializer = MiniMessage.miniMessage()

    fun templateSlug(templateId: String): String? = when (templateId.lowercase(Locale.ROOT)) {
        "huangjie_1c1boss" -> "huangjie_1c1boss"
        "huangjie_1c2boss" -> "huangjie_1c2boss"
        else -> null
    }

    fun legacyToMiniMessage(value: String): String =
        miniMessageSerializer.serialize(legacySerializer.deserialize(value))

    fun segmentRatio(boss: FirstFloorBossHudSnapshot?, segment: Int): Double {
        if (boss == null || segment !in 1..10 || !boss.health.isFinite() ||
            !boss.maxHealth.isFinite() || boss.maxHealth <= 0.0) return 0.0
        val progress = (boss.health / boss.maxHealth).coerceIn(0.0, 1.0)
        return (progress * 10.0 - (segment - 1)).coerceIn(0.0, 1.0)
    }
}

/** Captures Bukkit entity data on the server thread for BetterHud's placeholder evaluations. */
internal object FirstFloorBossHudTracker {
    private const val RADIUS = 32.0
    @Volatile private var published = emptyMap<UUID, FirstFloorBossHudSnapshot>()
    private var task: BukkitTask? = null
    private var failureLogged = false

    fun snapshot(playerId: UUID): FirstFloorBossHudSnapshot? = published[playerId]

    fun start() {
        if (task != null) return
        task = Bukkit.getScheduler().runTaskTimer(BOOTSTRAP as Plugin, ::scanSafely, 10L, 10L)
    }

    fun stop() {
        task?.cancel()
        task = null
        published = emptyMap()
        failureLogged = false
    }

    private fun scanSafely() {
        try {
            if (!Bukkit.isPrimaryThread()) return
            val next = HashMap<UUID, FirstFloorBossHudSnapshot>()
            for (player in Bukkit.getOnlinePlayers()) {
                if (!player.isOnline) continue
                val origin = player.location
                val nearest = player.getNearbyEntities(RADIUS, RADIUS, RADIUS)
                    .asSequence()
                    .filterIsInstance<LivingEntity>()
                    .filter { it.isValid && !it.isDead && it.world == player.world }
                    .mapNotNull { living ->
                        val distance = origin.distanceSquared(living.location)
                        if (distance > RADIUS * RADIUS) return@mapNotNull null
                        val type = MythicBukkit.inst().mobManager.getMythicMobInstance(living)?.mobType
                            ?: return@mapNotNull null
                        val slug = FirstFloorBossHudMath.templateSlug(type) ?: return@mapNotNull null
                        val maxHealth = living.getAttribute(Attribute.MAX_HEALTH)?.value ?: return@mapNotNull null
                        val health = living.health
                        if (!maxHealth.isFinite() || maxHealth <= 0.0 || !health.isFinite()) return@mapNotNull null
                        val snapshot = FirstFloorBossHudSnapshot(slug, FirstFloorBossHudMath.legacyToMiniMessage(living.name),
                            health.coerceIn(0.0, maxHealth), maxHealth)
                        Triple(distance, living.uniqueId, snapshot)
                    }
                    .minWithOrNull(compareBy<Triple<Double, UUID, FirstFloorBossHudSnapshot>> { it.first }
                        .thenBy { it.second })
                if (nearest != null) next[player.uniqueId] = nearest.third
            }
            published = next
            failureLogged = false
        } catch (exception: RuntimeException) {
            published = emptyMap()
            if (!failureLogged) {
                Bukkit.getLogger().warning("[BetterHud] First-floor Boss HUD scan failed: ${exception.message}")
                failureLogged = true
            }
        }
    }
}
