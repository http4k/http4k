package todo

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.RealBrowser
import org.http4k.fanbelt.testing.posting
import org.http4k.fanbelt.testing.start
import org.http4k.fanbelt.testing.verdict
import org.http4k.server.Jetty
import org.http4k.server.uri
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import todo.domain.TodoList
import todo.domain.TodoTitle
import todo.store.Lists

class PlaywrightTodoDriver(private val storage: DocumentStorage = InMemoryDocumentStorage()) : TodoDriver, AutoCloseable {
    private val app = App(AppRuntime()) { AppInfra(it, storage) }
    private val server = app.asServer(Jetty(0)).start()
    private val browser = RealBrowser(server.uri()).apply { start() }

    init {
        Lists(storage).create(TenantId.of("acme"), TodoList(start))
    }

    override fun user(name: String): User = PlaywrightUser()

    override fun close() {
        browser.close()
        server.stop()
    }

    private inner class PlaywrightUser : User {
        private val page: Page by lazy { browser.open("/lists/acme/") }

        override fun watches() {
            page
        }

        override fun adds(title: String) = page.verdict(".error") {
            page.locator("input.new").fill(title)
            page.locator("input.new").press("Enter")
        }

        override fun toggles(title: TodoTitle) {
            page.posting { item(title).locator("input[type=checkbox]").click() }
        }

        override fun renames(title: TodoTitle, to: String) = page.verdict(".error") {
            item(title).locator("input.title").fill(to)
            item(title).locator("input.title").press("Tab")
        }

        override fun deletes(title: TodoTitle) {
            page.posting { item(title).locator("button[aria-label='Delete']").click() }
        }

        override fun sees() = ListSeen(
            page.locator("#todos li").all().map { it.locator("input.title").inputValue() to it.getAttribute("class").orEmpty().contains("done") },
            page.locator("#todos .remaining").innerText().removeSuffix(" left").toInt(),
        )

        private fun item(title: TodoTitle): Locator = page.locator("#todos li:has(input.title[value=\"${title.value}\"])").first()
    }
}

@EnabledIfEnvironmentVariable(named = "FANBELT_TEST_BROWSER", matches = ".+")
class TodoPlaywrightTest : TodoContract() {
    override val driver = PlaywrightTodoDriver()

    @AfterEach
    fun close() = driver.close()
}