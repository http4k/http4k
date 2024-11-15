package org.http4k.connect.amazon.dynamodb.model

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import org.http4k.core.Uri

@JvmName("valueUri")
fun <VALUE : Value<Uri>> Attribute.Companion.value(vf: ValueFactory<VALUE, Uri>) = uri().value(vf)
