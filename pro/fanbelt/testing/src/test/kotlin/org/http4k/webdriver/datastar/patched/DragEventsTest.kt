/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.webdriver.datastar.patched

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.webdriver.datastar.DatastarWebDriver
import org.http4k.webdriver.datastar.UnsupportedDatastarExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.openqa.selenium.By
import org.openqa.selenium.Keys

class DragEventsTest {
    private fun browser(attributes: String) = DatastarWebDriver(
        routes("/" bind GET to { Response(OK).body("""<html><body $attributes><p id="count" data-text="${'$'}count"></p></body></html>""") }),
        plugins = listOf(DragEvents),
    )

    @Test
    fun `a page listening for a drag loads`() {
        val browser = browser("""data-on:dragenter__window="${'$'}dropping = true" data-on:dragover="${'$'}dropping = true" data-on:dragleave="${'$'}dropping = false" data-on:drop__delay.1ms="${'$'}dropping = false"""")

        assertDoesNotThrow { browser.get("http://localhost/") }
    }

    @Test
    fun `the events the driver already models still fire`() {
        val browser = browser("""data-signals="{count: 0}" data-on:click="${'$'}count = 1"""")

        browser.get("http://localhost/")
        browser.findElement(By.tagName("body")).click()

        assertEquals("1", browser.findElement(By.id("count")).text)
    }

    @Test
    fun `a key handler can prevent the default for one key and carry on`() {
        val browser = browser("""data-signals="{count: 0}" data-on:keydown="evt.key === 'Tab' && (evt.preventDefault(), ${'$'}count++)"""")

        browser.get("http://localhost/")
        browser.findElement(By.tagName("body")).sendKeys(Keys.TAB)

        assertEquals("1", browser.findElement(By.id("count")).text)
    }

    @Test
    fun `load still fires`() {
        val browser = browser("""data-signals="{count: 0}" data-on:load="${'$'}count = 1"""")

        browser.get("http://localhost/")

        assertEquals("1", browser.findElement(By.id("count")).text)
    }

    @Test
    fun `an event nothing models is still refused`() {
        assertThrows<UnsupportedDatastarExpression> { browser("""data-on:scroll="${'$'}count = 1"""").get("http://localhost/") }
    }

    @Test
    fun `listening on the window for an event the driver can't fire there is still refused`() {
        assertThrows<UnsupportedDatastarExpression> { browser("""data-on:click__window="${'$'}count = 1"""").get("http://localhost/") }
    }
}
