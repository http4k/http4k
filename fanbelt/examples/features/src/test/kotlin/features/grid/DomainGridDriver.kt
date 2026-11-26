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
            here = tenant { grid.arrive.open(id) }
            tenant { grid.here.read() }
        }

        override fun leaves() {
            here?.close()
            tenant { settle() }
        }

        override fun selects(cell: CellRef) {
            tenant { grid.here.carry(id, cell) }
        }

        override fun sets(cell: CellRef, text: String) = tenant { grid.set.execute(id, cell to text) }

        override fun sees(cell: CellRef) = tenant { grid.row.read(cell.row)[cell] ?: CellText.of("") }

        override fun cursors() = tenant { grid.here.read().filterKeys { it != id }.entries.mapNotNull { (who, ref) -> if (ref == null) null else ref to Viewer(who).name }.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() } }

        override fun here() = tenant { grid.here.read().keys.map { Viewer(it).name }.toSet() }
    }
}