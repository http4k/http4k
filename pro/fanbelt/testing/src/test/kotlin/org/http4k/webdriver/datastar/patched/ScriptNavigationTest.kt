/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.webdriver.datastar.patched

import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.MorphMode.append
import org.http4k.datastar.Selector
import org.http4k.lens.datastarElements
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.webdriver.datastar.DatastarWebDriver
import org.http4k.webdriver.datastar.UnsupportedDatastarExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.openqa.selenium.By

class ScriptNavigationTest {
    private fun page(script: String) = routes(
        "/lists/acme/" bind GET to {
            Response(OK).body(
                """<html><head><base href="/lists/acme/"></head><body>
                  <button data-on:click="@post('go')">Go</button><div id="goto"></div>
                </body></html>"""
            )
        },
        "/lists/acme/go" bind POST to { Response(OK).datastarElements(script, morphMode = append, selector = Selector.of("#goto")) },
        "/lists/acme/todos/7" bind GET to { Response(OK).body("""<html><body><p id="arrived">todo 7</p></body></html>""") },
    )

    private fun browser(script: String) = DatastarWebDriver(page(script), plugins = listOf(ScriptNavigation)).apply {
        get("http://localhost/lists/acme/")
    }

    @Test
    fun `a patched script that assigns the location navigates, relative to the page's base`() {
        val browser = browser("<script>window.location.assign('todos/7')</script>")

        browser.findElement(By.tagName("button")).click()

        assertEquals("/lists/acme/todos/7", browser.currentUrl?.substringAfter("localhost"))
        assertEquals("todo 7", browser.findElement(By.id("arrived")).text)
    }

    @Test
    fun `setting href navigates the same way`() {
        val browser = browser("<script>window.location.href = 'todos/7'</script>")

        browser.findElement(By.tagName("button")).click()

        assertEquals("todo 7", browser.findElement(By.id("arrived")).text)
    }

    @Test
    fun `any other script fails, rather than being ignored`() {
        val browser = browser("<script>alert('hi')</script>")

        assertThrows<UnsupportedDatastarExpression> { browser.findElement(By.tagName("button")).click() }
    }
}
