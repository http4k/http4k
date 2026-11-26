package features.grid

import org.http4k.lens.Path
import org.http4k.lens.value

val cellRef = Path.value(CellRef).of("cell")