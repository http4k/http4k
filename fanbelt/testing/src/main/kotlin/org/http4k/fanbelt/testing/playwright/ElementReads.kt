package org.http4k.fanbelt.testing.playwright

import com.microsoft.playwright.Locator
import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory

/**
 * Parses the located element's text as a value type.
 */
fun <T : Value<P>, P : Any> Locator.textAs(type: ValueFactory<T, P>): T = type.parse(innerText())

/**
 * Reads the located element's text as an enum constant.
 */
inline fun <reified E : Enum<E>> Locator.textAs(): E = enumValueOf(textContent().orEmpty().trim())

/**
 * Parses the named attribute as a value type.
 */
fun <T : Value<P>, P : Any> Locator.attributeAs(name: String, type: ValueFactory<T, P>): T = type.parse(attribute(name))

/**
 * Reads the named attribute as an enum constant.
 */
inline fun <reified E : Enum<E>> Locator.attributeAs(name: String): E = enumValueOf(attribute(name))

/**
 * The named attribute's value, failing if it is missing or empty.
 */
fun Locator.attribute(name: String) = checkNotNull(getAttribute(name)?.ifEmpty { null }) { "$this has no $name" }
