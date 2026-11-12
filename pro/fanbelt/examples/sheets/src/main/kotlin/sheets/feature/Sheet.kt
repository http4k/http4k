package sheets.feature

import sheets.domain.Cell
import sheets.domain.CellRef
import sheets.domain.CellText
import sheets.domain.RowNumber
import sheets.store.SheetTables

class Sheet(private val tables: SheetTables) {
    fun row(number: RowNumber) = tables.cells.page().filter { it.id.row == number }.associate { it.id to it.text }

    fun set(ref: CellRef, text: CellText) = when {
        text.value.isEmpty() -> tables.cells.delete(ref)
        tables.cells.get(ref) == null -> tables.cells.insert(Cell(ref, text))
        else -> tables.cells.update(ref) { it.copy(text = text) }
    }
}