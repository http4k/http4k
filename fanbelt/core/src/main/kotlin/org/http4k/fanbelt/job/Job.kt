package org.http4k.fanbelt.job

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.tenancy.Scope

/**
 * Work on one key, run in the background once an operation names it.
 */
class Job<Key : Value<*>> internal constructor(
    internal val name: String,
    private val keys: ValueFactory<Key, *>,
    private val run: (Scope, Key) -> Unit,
) {
    fun topic(key: Key): Topic = JobTopic.of(name, keys.show(key))

    internal fun runIn(scope: Scope, topic: JobTopic) =
        runCatching { keys.parse(topic.key) }.fold(
            { key -> runCatching { run(scope, key) }.onFailure { scope.events(JobFailed(name, key, it)) }.isSuccess },
            { scope.events(JobFailed(name, topic.key, it)); false },
        )
}
