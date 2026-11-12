package sheets

import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.testing.TestTenant
import sheets.domain.CellRef
import sheets.domain.CellText
import sheets.domain.Viewer
import sheets.feature.SheetFeature

class DomainSheetDriver : SheetDriver {
    private val tenant = TestTenant()

    override fun user(name: String): SheetUser = object : SheetUser {
        private val id = viewerNamed(name)
        private var here: Subscription? = null
        override val name = Viewer(id).name

        override fun watches() {
            here = SheetFeature.arrive(tenant.scope, id)
            tenant.settle()
            tenant.read(SheetFeature.here)
        }

        override fun leaves() {
            here?.close()
        }

        override fun selects(cell: CellRef) {
            tenant.set(SheetFeature.here, id, cell)
        }

        override fun sets(cell: CellRef, text: String) = tenant.execute(SheetFeature.set, id, cell to text)

        override fun sees(cell: CellRef) = tenant.read(SheetFeature.row, cell.row)[cell] ?: CellText.of("")

        override fun cursors() = tenant.read(SheetFeature.here).filterKeys { it != id }.entries.mapNotNull { (who, ref) -> if (ref == null) null else ref to Viewer(who).name }.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }

        override fun here() = tenant.read(SheetFeature.here).keys.map { Viewer(it).name }.toSet()
    }
}