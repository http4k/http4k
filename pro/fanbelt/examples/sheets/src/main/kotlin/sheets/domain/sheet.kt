package sheets.domain

import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.ValueFactory
import dev.forkhandles.values.length
import org.http4k.fanbelt.store.Identified
import java.time.Instant

val COLUMNS = ('A'..'J').toList()

const val ROWS = 20

private val cellNames = COLUMNS.flatMap { column -> (1..ROWS).map { row -> "$column$row" } }.toSet()

class CellRef private constructor(value: String) : StringValue(value) {
    val row get() = RowNumber.of(value.drop(1).toInt())

    companion object : StringValueFactory<CellRef>(::CellRef, { it in cellNames }) {
        fun of(column: Char, row: Int) = of("$column$row")
    }
}

val START: CellRef get() = CellRef.of('A', 1)

class RowNumber private constructor(value: Int) : IntValue(value) {
    companion object : IntValueFactory<RowNumber>(::RowNumber, { it in 1..ROWS })
}

class CellText private constructor(value: String) : StringValue(value) {
    companion object : ValueFactory<CellText, String>(::CellText, (0..200).length, String::trim)
}

data class Cell(override val id: CellRef, val text: CellText) : Identified<CellRef>

data class SheetDetails(val createdAt: Instant, override val id: String = SELF) : Identified<String> {
    companion object {
        const val SELF = "self"
    }
}