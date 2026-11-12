/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import com.microsoft.playwright.Locator
import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import org.openqa.selenium.WebElement

fun <T : Value<P>, P : Any> WebElement.textAs(type: ValueFactory<T, P>): T = type.parse(text.orEmpty())

inline fun <reified E : Enum<E>> WebElement.textAs(): E = enumValueOf(text.orEmpty())

fun <T : Value<P>, P : Any> WebElement.attributeAs(name: String, type: ValueFactory<T, P>): T = type.parse(attribute(name))

inline fun <reified E : Enum<E>> WebElement.attributeAs(name: String): E = enumValueOf(attribute(name))

fun WebElement.attribute(name: String) = checkNotNull(getAttribute(name)?.ifEmpty { null }) { "$tagName has no $name" }

fun <T : Value<P>, P : Any> Locator.textAs(type: ValueFactory<T, P>): T = type.parse(innerText())

inline fun <reified E : Enum<E>> Locator.textAs(): E = enumValueOf(textContent().orEmpty().trim())

fun <T : Value<P>, P : Any> Locator.attributeAs(name: String, type: ValueFactory<T, P>): T = type.parse(attribute(name))

inline fun <reified E : Enum<E>> Locator.attributeAs(name: String): E = enumValueOf(attribute(name))

fun Locator.attribute(name: String) = checkNotNull(getAttribute(name)?.ifEmpty { null }) { "$this has no $name" }
