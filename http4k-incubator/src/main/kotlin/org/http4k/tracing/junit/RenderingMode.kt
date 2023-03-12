package org.http4k.tracing.junit

import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Determines if the the TracerBullet will render output
 */
enum class RenderingMode(val shouldRender: (ExtensionContext) -> Boolean) {
    Always({ true }),
    OnSuccess({ it.executionException.isEmpty })
}
