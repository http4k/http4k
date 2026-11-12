package sheets

import com.microsoft.playwright.Page
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.RealBrowser
import org.http4k.fanbelt.testing.eventually
import org.http4k.fanbelt.testing.posting
import org.http4k.fanbelt.testing.start
import org.http4k.fanbelt.testing.verdict
import org.http4k.server.Jetty
import org.http4k.server.uri
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import sheets.domain.CellRef
import sheets.domain.CellText
import sheets.domain.SheetDetails
import sheets.domain.Viewer
import sheets.store.Sheets
import sheets.web.viewerCookie

class PlaywrightSheetDriver(private val storage: DocumentStorage = InMemoryDocumentStorage()) : SheetDriver, AutoCloseable {
    private val app = App(AppRuntime(), { AppInfra(it, storage) })
    private val server = app.asServer(Jetty(0)).start()
    private val browser = RealBrowser(server.uri()).apply { start() }

    init {
        Sheets(storage).create(TenantId.of("acme"), SheetDetails(start))
    }

    override fun close() {
        browser.close()
        server.stop()
    }

    override fun user(name: String) = PlaywrightUser(name)

    inner class PlaywrightUser(named: String) : SheetUser {
        private val id = viewerNamed(named)
        private var page: Page? = null
        override val name = Viewer(id).name

        override fun watches() {
            visit()
        }

        override fun leaves() {
            page?.context()?.close()
            page = null
        }

        override fun selects(cell: CellRef) {
            visit().posting { visit().locator("#cell-${cell.value}").click() }
        }

        override fun sets(cell: CellRef, text: String) = visit().run {
            selects(cell)
            verdict(".error") {
                locator("#cell-${cell.value}").fill(text)
                locator("#cell-${cell.value}").press("Tab")
            }
        }

        override fun sees(cell: CellRef) = CellText.of(visit().locator("#cell-${cell.value}").inputValue())

        override fun cursors() = cursorsIn(visit().locator("#cursors").textContent().orEmpty(), id)

        override fun here() = visit().locator("#here span").allInnerTexts().toSet()

        fun presses(key: String) {
            visit().posting { visit().keyboard().press(key) }
        }

        fun types(cell: CellRef, text: CellText) {
            selects(cell)
            visit().locator("#cell-${cell.value}").pressSequentially(text.value)
        }

        fun typing(cell: CellRef) = CellText.of(visit().locator("#cell-${cell.value}").inputValue())

        private fun visit(): Page = page ?: browser.open("/sheets/acme/", viewerCookie(id)).also { page = it }
    }
}

@EnabledIfEnvironmentVariable(named = "FANBELT_TEST_BROWSER", matches = ".+")
class SheetPlaywrightTest : SheetContract() {
    override val driver = PlaywrightSheetDriver()

    private val a6 = CellRef.of("A6")
    private val b5 = CellRef.of("B5")
    private val c5 = CellRef.of("C5")
    private val c6 = CellRef.of("C6")
    private val j5 = CellRef.of("J5")
    private val elsewhere = CellText.of("elsewhere")
    private val halfTyped = CellText.of("half-typed")

    @AfterEach
    fun close() = driver.close()

    @Test
    fun `the arrow keys move your cursor`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        bob.watches()

        ann.selects(b5)
        ann.presses("ArrowDown")
        ann.presses("ArrowRight")

        eventually { assertEquals(mapOf(c6 to setOf(ann.name)), bob.cursors()) }
    }

    @Test
    fun `tab moves to the next cell in the row, wrapping to the next row, and shift-tab goes back`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        bob.watches()

        ann.selects(b5)
        ann.presses("Tab")
        eventually { assertEquals(mapOf(c5 to setOf(ann.name)), bob.cursors()) }

        ann.presses("Shift+Tab")
        eventually { assertEquals(mapOf(b5 to setOf(ann.name)), bob.cursors()) }

        ann.selects(j5)
        ann.presses("Tab")
        eventually { assertEquals(mapOf(a6 to setOf(ann.name)), bob.cursors()) }
    }

    @Test
    fun `what you're typing survives someone changing a cell in the same row`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")

        ann.types(b5, halfTyped)
        bob.sets(c5, elsewhere.value)

        eventually { assertEquals(elsewhere, ann.sees(c5)) }
        assertEquals(halfTyped, ann.typing(b5))
    }
}