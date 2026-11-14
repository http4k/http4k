/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus

/**
 * Moves topics between nodes, and nothing else: a Backplane does the local fan-out on top of it. It reports what it
 * receives, and says when it has reconnected so the Backplane can mark everything dirty; interested and uninterested
 * let a transport listen only for the interests a node has subscribers for.
 */
interface Transport : AutoCloseable {
    fun send(topic: Topic)

    fun start(received: (Topic) -> Unit, reconnected: () -> Unit)

    fun interested(interest: Interest) {}

    fun uninterested(interest: Interest) {}
}
