package board.store

import board.domain.BoardId
import board.domain.BuildId
import board.domain.CardId
import board.domain.CardText
import board.domain.Commit
import board.domain.MemberId
import board.domain.ViewerId
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.standardConfig
import org.http4k.format.value
import org.http4k.fanbelt.withFanbeltMappings

object BoardJson : ConfigurableMoshi(
    standardConfig()
        .withFanbeltMappings()
        .value(BoardId)
        .value(CardId)
        .value(CardText)
        .value(MemberId)
        .value(ViewerId)
        .value(BuildId)
        .value(Commit)
        .done()
)