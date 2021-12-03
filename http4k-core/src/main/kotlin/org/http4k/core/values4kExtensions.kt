package org.http4k.core

import dev.forkhandles.values.AbstractValue
import dev.forkhandles.values.Validation
import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import java.util.UUID

/**
 * Custom values4k extensions.
 */

typealias UriValue = AbstractValue<Uri>

open class UriValueFactory<DOMAIN : Value<Uri>>(
    fn: (Uri) -> DOMAIN, validation: Validation<Uri>? = null
) : ValueFactory<DOMAIN, Uri>(fn, validation, Uri::of)
