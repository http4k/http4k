/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus

internal object LocalTransport : Transport {
    override fun send(message: BackplaneMessage) {}

    override fun start(received: (BackplaneMessage) -> Unit, reconnected: () -> Unit) {}

    override fun close() {}
}
