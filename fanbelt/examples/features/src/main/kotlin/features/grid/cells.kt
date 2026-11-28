package features.grid

import dev.forkhandles.values.Constraint.Companion.Invalid
import dev.forkhandles.values.ConstraintProblem
import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory
import dev.forkhandles.values.Message
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.ValueFactory
import dev.forkhandles.values.between
import dev.forkhandles.values.length
import org.http4k.fanbelt.store.Identified

val COLUMNS = ('A'..'J').toList()

const val ROWS = 20

private val cellNames = COLUMNS.flatMap { column -> (1..ROWS).map { row -> "$column$row" } }.toSet()

class CellRef private constructor(value: String) : StringValue(value) {
    val row get() = RowNumber.of(value.drop(1).toInt())

    companion object : StringValueFactory<CellRef>(::CellRef, { it: String -> if (it in cellNames) null else ConstraintProblem(Invalid, Message("must be a cell on the grid"), it) }) {
        fun of(column: Char, row: Int) = of("$column$row")
    }
}

val START: CellRef get() = CellRef.of('A', 1)

class RowNumber private constructor(value: Int) : IntValue(value) {
    companion object : IntValueFactory<RowNumber>(::RowNumber, (1..ROWS).between)
}

class CellText private constructor(value: String) : StringValue(value) {
    companion object : ValueFactory<CellText, String>(::CellText, (0..200).length, String::trim)
}

data class Cell(override val id: CellRef, val text: CellText, val row: RowNumber = id.row) : Identified<CellRef>
