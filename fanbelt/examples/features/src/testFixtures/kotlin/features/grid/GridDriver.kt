package features.grid

import org.http4k.fanbelt.testing.Verdict

interface GridDriver {
    fun user(name: String): GridUser
}

interface GridUser {
    val name: String
    fun watches()
    fun leaves()
    fun selects(cell: CellRef)
    fun sets(cell: CellRef, text: String): Verdict
    fun sees(cell: CellRef): CellText
    fun cursors(): Map<CellRef, Set<String>>
    fun here(): Set<String>
}