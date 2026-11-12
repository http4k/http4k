/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.webdriver.datastar.DatastarWebDriver
import org.http4k.webdriver.datastar.UnsupportedDatastarExpression
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class EffectsTest {
    private fun browser(effect: String) = DatastarWebDriver(
        routes("/" bind GET to { Response(OK).body("""<html><body><input id="box" data-effect="$effect"></body></html>""") }),
        plugins = listOf(Effects),
    )

    @Test
    fun `focusing an input when a signal turns on is allowed`() {
        val browser = browser("\$editing && el.focus()")

        assertDoesNotThrow { browser.get("http://localhost/") }
    }

    @Test
    fun `showing the names of the files chosen is allowed`() {
        val browser = browser("el.textContent = \$_files.map(file => file.name).join(', ')")

        assertDoesNotThrow { browser.get("http://localhost/") }
    }

    @Test
    fun `any other effect is still refused`() {
        val browser = browser("\$editing && launchMissiles()")

        assertThrows<UnsupportedDatastarExpression> { browser.get("http://localhost/") }
    }
}
