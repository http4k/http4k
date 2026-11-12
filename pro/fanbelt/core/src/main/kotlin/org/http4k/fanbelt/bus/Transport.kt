/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus

interface Transport : AutoCloseable {
    fun send(message: BackplaneMessage)

    fun start(received: (BackplaneMessage) -> Unit, reconnected: () -> Unit)

    fun interested(interest: Interest) {}

    fun uninterested(interest: Interest) {}
}
