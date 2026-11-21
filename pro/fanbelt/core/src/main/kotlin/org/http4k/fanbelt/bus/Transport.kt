/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus

/**
 * Carries topics between nodes for a Backplane.
 */
interface Transport : AutoCloseable {
    fun send(topics: Set<Topic>)

    fun start(received: (Topic) -> Unit, reconnected: () -> Unit)

    fun interested(interest: Interest) {}

    fun uninterested(interest: Interest) {}
}
