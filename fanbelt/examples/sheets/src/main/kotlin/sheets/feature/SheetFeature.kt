package sheets.feature

import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.feature.check
import org.http4k.fanbelt.feature.orElse
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.state.carry
import org.http4k.fanbelt.state.join
import sheets.domain.CellRef
import sheets.domain.CellText
import sheets.domain.RowNumber
import sheets.domain.START
import sheets.domain.ViewerId
import sheets.store.SheetTables

object SheetFeature : Feature<Sheet, ViewerId>({ tables -> Sheet(SheetTables(tables)) }) {
    val row by state { sheet, number: RowNumber -> sheet.row(number) }

    val here by presence(ViewerId, carrying = CellRef)

    val arrive by visit { scope, viewer ->
        val joined = here.join(scope, viewer)
        if (scope.read(here)[viewer] == null) here.carry(scope, viewer, START)
        joined
    }

    val set by operation { sheet, _, (ref, raw): Pair<CellRef, String> ->
        val text = FieldName.of("edit").check(CellText, raw, "A cell holds up to 200 characters").orElse { return@operation it }
        sheet.set(ref, text)
        Changes.of(row.topic(ref.row))
    }
}
