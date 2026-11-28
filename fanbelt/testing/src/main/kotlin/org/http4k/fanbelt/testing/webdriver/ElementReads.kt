package org.http4k.fanbelt.testing.webdriver

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import org.openqa.selenium.WebElement

/**
 * Parses the element's text as a value type.
 */
fun <T : Value<P>, P : Any> WebElement.textAs(type: ValueFactory<T, P>): T = type.parse(text)

/**
 * Reads the element's text as an enum constant.
 */
inline fun <reified E : Enum<E>> WebElement.textAs(): E = enumValueOf(text)

/**
 * Parses the named attribute as a value type.
 */
fun <T : Value<P>, P : Any> WebElement.attributeAs(name: String, type: ValueFactory<T, P>): T = type.parse(attribute(name))

/**
 * Reads the named attribute as an enum constant.
 */
inline fun <reified E : Enum<E>> WebElement.attributeAs(name: String): E = enumValueOf(attribute(name))

fun WebElement.attribute(name: String) = checkNotNull(getAttribute(name)?.ifEmpty { null }) { "$tagName has no $name" }
