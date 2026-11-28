package features.grid

import features.ViewerId
import org.http4k.fanbelt.page.PageShell
import org.http4k.template.ViewModel

data class EditSignals(val edit: String = "", val cursor: String = START.value)

data class CellView(val ref: CellRef, val text: String)

data class RowView(val number: Int, val cells: List<CellView>) : ViewModel

data class HereView(val names: List<String>) : ViewModel

data class CursorRule(val body: String, val cell: CellRef, val names: String)

data class CursorsView(val cursors: List<CursorRule>) : ViewModel

data class GridPageView(val shell: PageShell, val viewer: ViewerId, val columns: List<Char>, val rows: List<String>) : ViewModel
