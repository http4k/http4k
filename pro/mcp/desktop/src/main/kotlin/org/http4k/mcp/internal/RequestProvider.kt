package org.http4k.mcp.internal

import org.http4k.core.Request

fun interface RequestProvider : () -> Request
