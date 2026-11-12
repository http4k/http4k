package org.http4k.fanbelt.load

import kotlin.system.exitProcess

private val scenarios = mapOf(
    "fanout" to { fanout() },
    "presence-churn" to { presenceChurn() },
    "hot-tenant" to { hotTenant() },
    "memory" to { memory() },
)

fun main(args: Array<String>) {
    val chosen = args.ifEmpty { scenarios.keys.toTypedArray() }
    chosen.forEach { name -> checkNotNull(scenarios[name]) { "no scenario $name; choose from ${scenarios.keys}" }() }
    exitProcess(0)
}
