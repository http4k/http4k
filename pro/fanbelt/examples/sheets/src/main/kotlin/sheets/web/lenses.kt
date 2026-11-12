package sheets.web

import org.http4k.lens.Path
import org.http4k.lens.value
import sheets.domain.CellRef

val cellRef = Path.value(CellRef).of("cell")