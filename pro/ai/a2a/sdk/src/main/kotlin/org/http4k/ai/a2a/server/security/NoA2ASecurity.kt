/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server.security

import org.http4k.core.Filter
import org.http4k.core.NoOp

object NoA2ASecurity : A2ASecurity {
    override val filter = Filter.NoOp
}
