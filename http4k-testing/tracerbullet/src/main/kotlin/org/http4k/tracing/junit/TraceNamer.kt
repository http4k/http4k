package org.http4k.tracing.junit

import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Responsible for giving names to Traces
 */
fun interface TraceNamer : (ExtensionContext) -> String {
    companion object {
        val TestNameAndMethod = TraceNamer {
            it.testClass.map { it.simpleName + " - " }.orElse("") +
                it.displayName.substringBeforeLast("(")
        }
    }
}
