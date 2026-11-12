package sheets.store

import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.invoke
import sheets.domain.Cell
import sheets.domain.CellRef
import sheets.domain.SheetDetails

class SheetTables(tables: Tables) {
    val cells = tables<CellRef, Cell>(CELLS)
    val details = tables<String, SheetDetails>(SHEETS)

    companion object {
        val CELLS = TableName.of("cells")
        val SHEETS = TableName.of("sheet")
        val all = listOf(CELLS, SHEETS)
    }
}