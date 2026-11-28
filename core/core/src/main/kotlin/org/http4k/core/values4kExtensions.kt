package org.http4k.core

import dev.forkhandles.values.AbstractValue
import dev.forkhandles.values.Validator
import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory

/**
 * Custom values4k extensions.
 */

typealias UriValue = AbstractValue<Uri>

open class UriValueFactory<DOMAIN : Value<Uri>>(
    fn: (Uri) -> DOMAIN, validation: Validator<Uri>? = null
) : ValueFactory<DOMAIN, Uri>(fn, validation, Uri::of)
