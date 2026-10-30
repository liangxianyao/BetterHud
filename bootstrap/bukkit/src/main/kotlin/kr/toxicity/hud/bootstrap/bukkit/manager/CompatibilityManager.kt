package kr.toxicity.hud.bootstrap.bukkit.manager

import kr.toxicity.hud.api.update.UpdateEvent
import kr.toxicity.hud.bootstrap.bukkit.compatibility.Compatibility
import kr.toxicity.hud.bootstrap.bukkit.compatibility.craftengine.CraftEngineCompatibility
import kr.toxicity.hud.bootstrap.bukkit.compatibility.mmocore.MMOCoreCompatibility
import kr.toxicity.hud.bootstrap.bukkit.compatibility.mmoitems.MMOItemsCompatibility
import kr.toxicity.hud.bootstrap.bukkit.compatibility.mythiclib.MythicLibCompatibility
import kr.toxicity.hud.bootstrap.bukkit.compatibility.mythicmobs.MythicMobsCompatibility
import kr.toxicity.hud.bootstrap.bukkit.compatibility.nexo.NexoCompatibility
import kr.toxicity.hud.bootstrap.bukkit.compatibility.parties.PartiesCompatibility
import kr.toxicity.hud.bootstrap.bukkit.compatibility.skript.SkriptCompatibility
import kr.toxicity.hud.bootstrap.bukkit.compatibility.vault.VaultCompatibility
import kr.toxicity.hud.bootstrap.bukkit.compatibility.worldguard.WorldGuardCompatibility
import kr.toxicity.hud.util.PLUGIN
import kr.toxicity.hud.util.handleFailure
import org.bukkit.Bukkit
import java.util.function.Function

object CompatibilityManager {

    private val active = mutableListOf<Compatibility>()

    val compatibilities = mapOf(
        "MMOCore" to {
            MMOCoreCompatibility()
        },
        "MythicMobs" to {
            MythicMobsCompatibility()
        },
        "WorldGuard" to {
            WorldGuardCompatibility()
        },
        "Vault" to {
            VaultCompatibility()
        },
        "MythicLib" to {
            MythicLibCompatibility()
        },
        "Skript" to {
            SkriptCompatibility()
        },
        "MMOItems" to {
            MMOItemsCompatibility()
        },
        "Parties" to {
            PartiesCompatibility()
        },
        "CraftEngine" to {
            CraftEngineCompatibility()
        },
        "Nexo" to {
            NexoCompatibility()
        }
    )

    fun start() {
        if (active.isNotEmpty()) return
        compatibilities.forEach { compatibility ->
            if (Bukkit.getPluginManager().isPluginEnabled(compatibility.key)) {
                val obj = compatibility.value()
                runCatching {
                    obj.start()
                    val namespace = compatibility.key.lowercase()
                    obj.listeners.forEach { entry ->
                        PLUGIN.listenerManager.addListener("${namespace}_${entry.key}") { c ->
                            val reason = entry.value(c)
                            Function { u: UpdateEvent ->
                                reason(u)
                            }
                        }
                    }
                    obj.numbers.forEach { entry ->
                        PLUGIN.placeholderManager.numberContainer.addPlaceholder("${namespace}_${entry.key}", entry.value)
                    }
                    obj.strings.forEach { entry ->
                        PLUGIN.placeholderManager.stringContainer.addPlaceholder("${namespace}_${entry.key}", entry.value)
                    }
                    obj.booleans.forEach { entry ->
                        PLUGIN.placeholderManager.booleanContainer.addPlaceholder("${namespace}_${entry.key}", entry.value)
                    }
                    obj.triggers.forEach { entry ->
                        PLUGIN.triggerManager.addTrigger("${namespace}_${entry.key}", entry.value)
                    }
                }.onSuccess {
                    active += obj
                }.onFailure {
                    runCatching { obj.stop() }.handleFailure { "Unable to stop ${compatibility.key} support after startup failure." }
                }.handleFailure {
                    "Unable to load ${compatibility.key} support. checks this: ${obj.website}"
                }
            }
        }
    }

    fun stop() {
        active.asReversed().forEach { compatibility ->
            runCatching { compatibility.stop() }.handleFailure {
                "Unable to stop ${compatibility.javaClass.simpleName} support."
            }
        }
        active.clear()
    }
}
