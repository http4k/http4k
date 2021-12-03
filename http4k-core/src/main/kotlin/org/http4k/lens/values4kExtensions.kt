package org.http4k.lens

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory

/**
 * Custom lens for mapping Path to values4k Value
 */
fun <VALUE : Value<T>, T : Any> Path.value(vf: ValueFactory<VALUE, T>) = map(vf::parse, vf::show)

/**
 * Custom lens for mapping to values4k Value
 */
fun <M : Any, VALUE : Value<T>, T : Any> BiDiLensSpec<M, String>.value(vf: ValueFactory<VALUE, T>) = map(vf::parse, vf::show)
