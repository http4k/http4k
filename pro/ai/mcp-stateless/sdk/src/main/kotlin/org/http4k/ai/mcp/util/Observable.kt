/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.util

import org.http4k.ai.mcp.server.protocol.ObservableCapability
import org.http4k.ai.mcp.server.protocol.Session
import java.util.concurrent.ConcurrentHashMap

abstract class Observable<T> : ObservableCapability<T> {

    protected val callbacks = ConcurrentHashMap<Session, () -> Any>()

    override fun onChange(session: Session, handler: () -> Any) {
        callbacks[session] = handler
    }

    override fun remove(session: Session) {
        callbacks.remove(session)
    }
}
