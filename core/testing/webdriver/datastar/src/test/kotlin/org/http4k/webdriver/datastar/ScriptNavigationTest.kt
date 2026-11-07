package org.http4k.webdriver.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.endsWith
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.DatastarEvent.PatchElements
import org.http4k.datastar.MorphMode
import org.http4k.datastar.Selector
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import org.openqa.selenium.By

class ScriptNavigationTest {

    private fun browser(script: String) = DatastarWebDriver(
        routes(
            "/lists/acme/" bind GET to {
                Response(OK).body("""<html><head><base href="/lists/acme/"></head><body><button id="go" data-on:click="@post('go')">go</button><div id="goto"></div></body></html>""")
            },
            "/lists/acme/go" bind POST to {
                Response(OK).body(sseBody(PatchElements(script, morphMode = MorphMode.append, selector = Selector.of("#goto")).toSseEvent()))
            },
            "/lists/acme/todos/7" bind GET to { Response(OK).body("""<html><body><p id="arrived">todo 7</p></body></html>""") },
        ),
        plugins = listOf(ScriptNavigation)
    ).apply { get("/lists/acme/") }

    @Test
    fun `a patched script that assigns the location navigates, relative to the page's base`() {
        val driver = browser("<script>window.location.assign('todos/7')</script>")

        driver.findElement(By.id("go")).click()

        assertThat(driver.currentUrl.orEmpty(), endsWith("/lists/acme/todos/7"))
        assertThat(driver.findElement(By.id("arrived")).text, equalTo("todo 7"))
    }

    @Test
    fun `setting href navigates the same way`() {
        val driver = browser("<script>window.location.href = 'todos/7'</script>")

        driver.findElement(By.id("go")).click()

        assertThat(driver.findElement(By.id("arrived")).text, equalTo("todo 7"))
    }

    @Test
    fun `any other script fails, rather than being ignored`() {
        val driver = browser("<script>alert('hi')</script>")

        assertThat({ driver.findElement(By.id("go")).click() }, throws<UnsupportedDatastarExpression>())
    }
}
