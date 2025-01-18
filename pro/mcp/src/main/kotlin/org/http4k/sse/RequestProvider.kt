package org.http4k.sse

import org.http4k.core.Request

fun interface RequestProvider : () -> Request
