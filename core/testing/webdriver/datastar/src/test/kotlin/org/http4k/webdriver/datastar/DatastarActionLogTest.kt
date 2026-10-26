package org.http4k.webdriver.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import org.openqa.selenium.By

class DatastarActionLogTest {

    private val home = """
        <html><body data-signals="{q: 'x'}" data-init="@get('/things/init')">
            <button id='save' data-on:click="@post('save')">save</button>
            <a id='away' href='/elsewhere'>away</a>
        </body></html>
    """.trimIndent()

    private val app = routes(
        "/things/page" bind GET to { Response(OK).body(home) },
        "/things/init" bind GET to { Response(NO_CONTENT) },
        "/things/save" bind POST to { Response(FORBIDDEN) },
        "/elsewhere" bind GET to { Response(OK).body("<html><body>elsewhere</body></html>") }
    )

    @Test
    fun `actions are logged in the order they fired with their method, full uri and status`() {
        val driver = driverFor(app)
        driver.get("/things/page")
        driver.findElement(By.id("save")).click()

        assertThat(
            driver.actions(),
            equalTo(
                listOf(
                    ActionResult(GET, Uri.of("/things/init").query("datastar", """{"q":"x"}"""), NO_CONTENT),
                    ActionResult(POST, Uri.of("/things/save"), FORBIDDEN)
                )
            )
        )
    }

    @Test
    fun `navigating away clears the action log`() {
        val driver = driverFor(app)
        driver.get("/things/page")
        driver.findElement(By.id("away")).click()

        assertThat(driver.actions(), equalTo(emptyList()))
    }

    @Test
    fun `the action log survives closing the browser`() {
        val driver = driverFor(app)
        driver.get("/things/page")
        driver.close()

        assertThat(driver.actions().map { it.status }, equalTo(listOf(NO_CONTENT)))
    }
}
