/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.input

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.fanbelt.feature.Outcome.Rejected

interface Feedback<in Result : Any> {
    fun rejected(rejected: Rejected): Response
    fun accepted(request: Request, result: Result, reset: Map<String, Any?>): Response
}
