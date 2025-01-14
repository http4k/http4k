package org.http4k.mcp

import org.http4k.core.Status

sealed interface ProcessResult {
    data object Ok : ProcessResult
    data class Fail(val status: Status) : ProcessResult
}
