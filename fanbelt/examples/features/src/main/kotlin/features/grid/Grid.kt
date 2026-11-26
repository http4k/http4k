package features.grid

import org.http4k.fanbelt.store.eq

class Grid(private val tables: GridTables) {
    fun all() = tables.cells.page().items.sortedWith(compareBy({ it.row.value }, { it.id.value.first() }))

    fun row(number: RowNumber) = tables.cells.page(Cell::row eq number).items.associate { it.id to it.text }

    fun set(ref: CellRef, text: CellText) = when {
        text.value.isEmpty() -> tables.cells.delete(ref)
        tables.cells.get(ref) == null -> tables.cells.insert(Cell(ref, text))
        else -> tables.cells.update(ref) { it.copy(text = text) }
    }
}
