package org.http4k.format

import dev.forkhandles.data.MoshiNodeDataContainer
import org.http4k.core.Body
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.ContentNegotiation

/**
 * Custom lens to extract and inject Data4k DataContainer types from JSON bodies
 */
fun <T : MoshiNodeDataContainer> Body.Companion.json(
    fn: (MoshiNode) -> T,
    description: String? = null,
    contentNegotiation: ContentNegotiation = ContentNegotiation.None
): BiDiBodyLensSpec<T> = Moshi.body(description, contentNegotiation).map(fn, MoshiNodeDataContainer::unwrap)

