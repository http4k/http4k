package sheets

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.cookie.cookie
import org.http4k.fanbelt.testing.InMemoryCluster
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Test
import sheets.domain.CellRef
import sheets.domain.Viewer
import sheets.web.viewerCookie

class SheetMultiNodeTest : SheetContract() {
    private val cluster = InMemoryCluster()
    private val nodes = List(2) { App(cluster.runtime, cluster.node()) }

    override val driver = UiSheetDriver(cluster.runtime.scheduler, cluster.storage, nodes)

    private fun movesCursor(name: String, to: CellRef, onNode: Int) {
        nodes[onNode].handler.http!!(Request(POST, "/sheets/acme/cursor/$to").cookie(viewerCookie(viewerNamed(name))))
        cluster.runtime.scheduler.runUntilIdle()
    }

    @Test
    fun `a cursor moved through one node follows a viewer whose page is open on another`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        ann.watches()
        bob.watches()

        movesCursor("ann", CellRef.of("B5"), onNode = 1)

        eventually { assertThat(bob.cursors(), equalTo(mapOf(CellRef.of("B5") to setOf(ann.name)))) }
    }

    @Test
    fun `a cursor from someone with no page open is ignored, on whichever node it lands`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        ann.watches()
        bob.watches()

        movesCursor("eve", CellRef.of("B5"), onNode = 0)
        movesCursor("eve", CellRef.of("C7"), onNode = 1)

        assertThat(bob.cursors(), equalTo(mapOf(CellRef.of("A1") to setOf(ann.name))))
        assertThat(Viewer(viewerNamed("eve")).name in bob.cursors().values.flatten(), equalTo(false))
    }
}
