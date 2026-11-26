package sl4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.throws
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.webdriver.datastar.DatastarWebDriver
import org.http4k.webdriver.datastar.UnsupportedDatastarExpression
import org.junit.jupiter.api.Test

class EffectsTest {
    private fun browser(effect: String) = DatastarWebDriver(
        routes("/" bind GET to { Response(OK).body("""<html><body><input id="box" data-effect="$effect"></body></html>""") }),
        plugins = listOf(Effects),
    )

    @Test
    fun `focusing an input when a signal turns on is allowed`() {
        val browser = browser("\$editing && el.focus()")

        browser.get("http://localhost/")
    }

    @Test
    fun `showing the names of the files chosen is allowed`() {
        val browser = browser("el.textContent = \$_files.map(file => file.name).join(', ')")

        browser.get("http://localhost/")
    }

    @Test
    fun `any other effect is still refused`() {
        val browser = browser("\$editing && launchMissiles()")

        assertThat({ browser.get("http://localhost/") }, throws<UnsupportedDatastarExpression>())
    }
}
