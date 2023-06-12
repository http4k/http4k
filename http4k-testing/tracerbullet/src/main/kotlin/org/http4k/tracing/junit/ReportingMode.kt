package org.http4k.tracing.junit

import org.http4k.tracing.TraceCompletion
import org.http4k.tracing.TraceCompletion.complete
import org.http4k.tracing.TraceCompletion.incomplete

/**
 * Determines if the the TracerBullet will report a render
 */
fun interface ReportingMode : (TraceCompletion) -> Boolean {
    companion object {
        val Always = ReportingMode { true }
        val OnSuccess = ReportingMode { it == complete }
        val OnFailure = ReportingMode { it == incomplete }
    }
}

