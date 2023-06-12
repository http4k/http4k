package org.http4k.tracing.junit

import org.http4k.tracing.TraceCompletion
import org.http4k.tracing.TraceCompletion.complete
import org.http4k.tracing.TraceCompletion.incomplete

/**
 * Determines if the the TracerBullet will render output
 */
fun interface RenderingMode : (TraceCompletion) -> Boolean {
    companion object {
        val Always = RenderingMode { true }
        val OnSuccess = RenderingMode { it == complete }
        val OnFailure = RenderingMode { it == incomplete }
    }
}

