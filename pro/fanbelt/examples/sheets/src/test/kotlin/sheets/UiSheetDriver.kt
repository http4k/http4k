package sheets

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.Node
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.TestRuntime
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.browser
import org.http4k.fanbelt.testing.start
import org.http4k.fanbelt.testing.verdict
import org.openqa.selenium.By
import sheets.domain.CellRef
import sheets.domain.CellText
import sheets.domain.SheetDetails
import sheets.domain.Viewer
import sheets.store.Sheets
import sheets.web.viewerCookie

class UiSheetDriver(
    private val scheduler: DeterministicScheduler = DeterministicScheduler(start),
    private val storage: DocumentStorage = InMemoryDocumentStorage(),
    private val nodes: List<Node> = listOf(App(TestRuntime(scheduler), { AppInfra(it, storage) })),
) : SheetDriver {
    private var arrivals = 0

    init {
        nodes.forEach(Node::start)
        Sheets(storage).create(TenantId.of("acme"), SheetDetails(start))
    }

    override fun user(name: String): SheetUser = object : SheetUser {
        private val id = viewerNamed(name)
        private val browser = browser(nodes[arrivals++ % nodes.size], scheduler, viewerCookie(id))
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
            return browser.verdict(By.cssSelector(".error"))
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