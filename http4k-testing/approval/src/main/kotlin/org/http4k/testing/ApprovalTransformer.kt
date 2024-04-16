package org.http4k.testing

import java.io.InputStream

/**
 * Convert the output into a Comparable form.
 */
fun interface ApprovalTransformer<T> : (InputStream) -> T {
    companion object {
        /**
         * Use this for HTTP bodies which are strings with normalised line endings.
         */
        fun StringWithNormalisedLineEndings(): ApprovalTransformer<String> {
            val matchAllLineEndings = "\\r\\n?".toRegex()
            fun String.normalizeLineEndings() = replace(matchAllLineEndings, "\n")

            return ApprovalTransformer {
                it.reader().use { it.readText().normalizeLineEndings().trimEnd() }
            }
        }
    }
}
