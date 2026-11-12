package sheets.store

import org.http4k.format.ConfigurableMoshi
import org.http4k.format.standardConfig
import org.http4k.format.value
import org.http4k.fanbelt.withFanbeltMappings
import sheets.domain.CellRef
import sheets.domain.CellText
import sheets.domain.RowNumber
import sheets.domain.ViewerId

object SheetJson : ConfigurableMoshi(
    standardConfig()
        .withFanbeltMappings()
        .value(CellRef)
        .value(RowNumber)
        .value(CellText)
        .value(ViewerId)
        .done()
)