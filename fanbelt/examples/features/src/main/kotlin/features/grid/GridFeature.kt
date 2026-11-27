package features.grid

import features.ViewerId
import org.http4k.fanbelt.feature.Allowed
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.feature.check
import org.http4k.fanbelt.feature.orElse
import org.http4k.fanbelt.input.FieldName

class GridFeature : Feature<Grid, ViewerId>({ tables -> Grid(GridTables(tables)) }) {
    val row by state(RowNumber) { sheet, number -> sheet.row(number) }

    val cells by rule { ctx, _ -> Allowed(ctx.domain.all()) }

    val here by presence(ViewerId, carrying = CellRef)

    val arrive by visit { scope, viewer ->
        val joined = scope.join(here, viewer)
        if (scope.read(here)[viewer] == null) scope.carry(here, viewer, START)
        joined
    }

    val set by operation { sheet, _, (ref, raw): Pair<CellRef, String> ->
        val text = FieldName.of("edit").check(CellText, raw, "A cell holds up to 200 characters").orElse { return@operation it }
        sheet.set(ref, text)
        Changes.of(row.topic(ref.row))
    }
}
