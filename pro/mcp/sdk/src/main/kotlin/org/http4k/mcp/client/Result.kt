package org.http4k.mcp.client

import dev.forkhandles.result4k.Result4k
import java.lang.Exception

typealias McpResult<T> = Result4k<T, Exception>
