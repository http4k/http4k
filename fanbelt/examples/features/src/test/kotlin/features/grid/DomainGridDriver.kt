package features.grid

import features.Viewer
import features.viewerNamed
import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.testing.TestTenant

class DomainGridDriver : GridDriver {
    private val tenant = TestTenant()
    private val grid = GridFeature()

    override fun user(name: String): GridUser = object : GridUser {
        private val id = viewerNamed(name)
        private var here: Subscription? = null
        override val name = Viewer(id).name

        override fun watches() {
            here = tenant { open(grid.arrive, id) }
            tenant { read(grid.here) }
        }

        override fun leaves() {
            here?.close()
            tenant { settle() }
        }

        override fun selects(cell: CellRef) {
            tenant { carry(grid.here, id, cell) }
        }

        override fun sets(cell: CellRef, text: String) = tenant { execute(grid.set, id, cell to text) }

        override fun sees(cell: CellRef) = tenant { read(grid.row, cell.row)[cell] ?: CellText.of("") }

        override fun cursors() = tenant { read(grid.here).filterKeys { it != id }.entries.mapNotNull { (who, ref) -> if (ref == null) null else ref to Viewer(who).name }.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() } }

        override fun here() = tenant { read(grid.here).keys.map { Viewer(it).name }.toSet() }
    }
}