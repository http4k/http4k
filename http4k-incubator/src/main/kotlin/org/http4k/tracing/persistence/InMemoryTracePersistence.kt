package org.http4k.tracing.persistence

import org.http4k.tracing.ScenarioTraces
import org.http4k.tracing.TracePersistence

fun TracePersistence.Companion.InMemory() = object : TracePersistence {
    private val list = mutableListOf<ScenarioTraces>()
    override fun store(trace: ScenarioTraces) {
        list += trace
    }

    override fun load() = list
}