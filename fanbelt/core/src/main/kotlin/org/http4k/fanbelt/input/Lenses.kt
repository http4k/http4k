package org.http4k.fanbelt.input

import org.http4k.lens.LensFailure

@PublishedApi
internal inline fun <T> lensOrNull(read: () -> T): T? = try {
    read()
} catch (_: LensFailure) {
    null
}
