package org.http4k.powerassert

import org.http4k.lens.Lens
import org.http4k.lens.LensFailure
import org.http4k.lens.WebForm

@Suppress("NOTHING_TO_INLINE")
inline fun <T> WebForm.hasFormField(lens: Lens<WebForm, T>, expected: T): Boolean = try {
    lens(this) == expected
} catch (e: LensFailure) {
    false
}