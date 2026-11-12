package board.web

import board.domain.BoardId
import board.domain.CardId
import board.domain.Direction
import board.domain.Role
import board.domain.ViewerId
import org.http4k.lens.Path
import org.http4k.lens.enum
import org.http4k.lens.value

val boardId = Path.value(BoardId).of("board")
val cardId = Path.value(CardId).of("card")
val viewerId = Path.value(ViewerId).of("viewer")
val direction = Path.enum<Direction>().of("direction")
val role = Path.enum<Role>().of("role")