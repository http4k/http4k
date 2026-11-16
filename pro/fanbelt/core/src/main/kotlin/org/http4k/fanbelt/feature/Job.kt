/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.feature

import org.http4k.fanbelt.bus.JobTopic
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.bus.job
import org.http4k.fanbelt.state.StoredState
import org.http4k.fanbelt.tenancy.Scope

/**
 * Work on one key of a stored state: an operation that names topic(key) in Changes records it, and any node running
 * Jobs runs it on that key's model, with no transaction open. It is done when run returns, and runs again if it throws.
 */
class Job<Key : Any, Model> internal constructor(
    internal val name: String,
    private val state: StoredState<*, Key, Model>,
    private val run: (Scope, Model) -> Unit,
) {
    fun topic(key: Key) = Topic.job(name, state.show(key))

    internal fun runIn(scope: Scope, topic: JobTopic) =
        runCatching { run(scope, scope.read(state, state.parse(topic.value.substringAfter('/')))) }.onFailure { scope.events(JobFailed(name, it)) }.isSuccess
}
