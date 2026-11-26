package features

import features.builds.BuildId
import features.builds.Commit
import features.notes.NoteId
import features.notes.NoteText
import features.rooms.LineId
import features.rooms.LineText
import features.rooms.RoomName
import features.rooms.RoomTitle
import features.rooms.SeenId
import features.grid.CellRef
import features.grid.CellText
import features.grid.RowNumber
import org.http4k.fanbelt.withFanbeltMappings
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.standardConfig
import org.http4k.format.value

object FeaturesJson : ConfigurableMoshi(
    standardConfig()
        .withFanbeltMappings()
        .value(SpaceDetailsId)
        .value(CellRef)
        .value(RowNumber)
        .value(CellText)
        .value(ViewerId)
        .value(BuildId)
        .value(Commit)
        .value(NoteId)
        .value(NoteText)
        .value(RoomName)
        .value(RoomTitle)
        .value(LineId)
        .value(LineText)
        .value(SeenId)
        .done()
)
