/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.input

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.fanbelt.withFanbeltMappings
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.standardConfig
import java.lang.reflect.Type

@PublishedApi
internal object SignalsJson : ConfigurableMoshi(standardConfig().withFanbeltMappings().done().add(SerializeNulls))

internal object SerializeNulls : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<in Any>? =
        moshi.nextAdapter<Any>(this, type, annotations).serializeNulls()
}
