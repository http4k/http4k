/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.sse

import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.lens.Query
import org.http4k.lens.value

val sessionId = Query.value(SessionId).optional("sessionId")
