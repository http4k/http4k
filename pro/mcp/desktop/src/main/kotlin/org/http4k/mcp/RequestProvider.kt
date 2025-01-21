package org.http4k.mcp

import org.http4k.core.Request

fun interface RequestProvider : () -> Request
