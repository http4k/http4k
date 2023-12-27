package org.http4k.lens

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import dev.forkhandles.values.parseOrNull

/**
 * Custom lens for mapping Path to values4k Value
 */
fun <VALUE : Value<T>, T : Any> BiDiPathLensSpec<String>.value(vf: ValueFactory<VALUE, T>) = map(vf::parse, vf::show)

/**
 * Custom lens for mapping to values4k Value
 */
fun <M : Any, VALUE : Value<T>, T : Any> BiDiLensSpec<M, String>.value(vf: ValueFactory<VALUE, T>) = map(vf::parse, vf::show)

/**
 * Convert ot throw a lens failure when constructing a value type
 */
fun <IN : Any, VALUE : Value<T>, T : Any> Lens<IN, String>.ofOrLensFailure(vf: ValueFactory<VALUE, T>, value: String)
= vf.parseOrNull(value) ?: throw LensFailure(Invalid(meta))
