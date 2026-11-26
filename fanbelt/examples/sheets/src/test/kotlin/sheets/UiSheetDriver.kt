package sheets

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.then
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.infrastructure.TestRuntime
import org.http4k.fanbelt.testing.webdriver.toFanBeltBrowser
import org.http4k.fanbelt.testing.webdriver.verdictAt
import org.http4k.filter.ClientFilters
import org.http4k.testing.toHttpHandler
import org.openqa.selenium.By
import sheets.domain.CellRef
import sheets.domain.CellText
import sheets.domain.SheetDetails
import sheets.domain.Viewer
import sheets.store.Sheets
import sheets.web.viewerCookie
import java.time.Instant.ofEpochMilli

class UiSheetDriver : SheetDriver {
    private val scheduler = DeterministicScheduler(ofEpochMilli(0))
    private val storage = InMemoryDocumentStorage()
    private val node = App(TestRuntime(scheduler), { AppInfra(it, storage) })

    init {
        node.start()
        Sheets(storage).create(TenantId.of("acme"), SheetDetails(ofEpochMilli(0)))
    }

    override fun user(name: String): SheetUser = object : SheetUser {
        private val id = viewerNamed(name)

        private val browser = ClientFilters.SetCookie(viewerCookie(id))
            .then(node.handler.toHttpHandler())
            .toFanBeltBrowser(scheduler)
        private var visited = false
        override val name = Viewer(id).name

        override fun watches() = visit()

        override fun leaves() {
            browser.close()
            scheduler.runUntilIdle()
            visited = false
        }

        override fun selects(cell: CellRef) {
            visit()
            browser.findElement(By.id("cell-${cell.value}")).click()
        }

        override fun sets(cell: CellRef, text: String): Verdict {
            selects(cell)
            browser.findElement(By.id("cell-${cell.value}")).run {
                clear()
                sendKeys(text)
            }
            return browser.verdictAt(By.cssSelector(".error"))
        }

        override fun sees(cell: CellRef): CellText {
            visit()
            return CellText.of(browser.findElement(By.id("cell-${cell.value}")).getAttribute("value").orEmpty())
        }

        override fun cursors(): Map<CellRef, Set<String>> {
            visit()
            return cursorsIn(browser.findElement(By.id("cursors")).getDomProperty("innerHTML").orEmpty(), id)
        }

        override fun here(): Set<String> {
            visit()
            return browser.findElements(By.cssSelector("#here span")).map { it.text }.toSet()
        }

        private fun visit() {
            if (visited) return
            browser.get("http://localhost/sheets/acme/")
            visited = true
        }
    }
}
