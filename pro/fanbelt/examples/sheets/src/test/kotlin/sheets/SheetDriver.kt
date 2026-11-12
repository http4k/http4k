package sheets

import org.http4k.fanbelt.testing.Verdict
import sheets.domain.CellRef
import sheets.domain.CellText

interface SheetDriver {
    fun user(name: String): SheetUser
}

interface SheetUser {
    val name: String
    fun watches()
    fun leaves()
    fun selects(cell: CellRef)
    fun sets(cell: CellRef, text: String): Verdict
    fun sees(cell: CellRef): CellText
    fun cursors(): Map<CellRef, Set<String>>
    fun here(): Set<String>
}