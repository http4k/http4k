package org.http4k.fanbelt.job

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.tenancy.Scope

/**
 * Work on one key: an operation that names topic(key) in Changes records it, and any node running Jobs runs it with that
 * key, with no transaction open. It is done when run returns, and runs again if it throws.
 */
class Job<Key : Value<*>> internal constructor(
    internal val name: String,
    private val keys: ValueFactory<Key, *>,
    private val run: (Scope, Key) -> Unit,
) {
    fun topic(key: Key): Topic = JobTopic.of(name, keys.show(key))

    internal fun runIn(scope: Scope, topic: JobTopic) =
        runCatching { run(scope, keys.parse(topic.key)) }.onFailure { scope.events(JobFailed(name, it)) }.isSuccess
}
