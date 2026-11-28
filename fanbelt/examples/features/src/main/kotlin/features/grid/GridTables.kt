package features.grid

import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.invoke

class GridTables(tables: Tables) {
    val cells = tables<CellRef, Cell>(CELLS)

    companion object {
        val CELLS = TableName.of("cells")
    }
}