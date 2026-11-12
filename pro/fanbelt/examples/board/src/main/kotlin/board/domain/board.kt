package board.domain

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.UUIDValue
import dev.forkhandles.values.UUIDValueFactory
import dev.forkhandles.values.ValueFactory
import dev.forkhandles.values.and
import dev.forkhandles.values.length
import dev.forkhandles.values.regex
import org.http4k.fanbelt.feature.check
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.store.Identified
import java.time.Instant
import java.util.UUID

class BoardId private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<BoardId>(::BoardId, "[a-z0-9-]{1,40}".regex)
}

class CardId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<CardId>(::CardId)
}

class CardText private constructor(value: String) : StringValue(value) {
    companion object : ValueFactory<CardText, String>(::CardText, (1..100).length.and { it.isNotBlank() }, String::trim)
}

class MemberId private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<MemberId>(::MemberId, (1..100).length)
}

enum class Column { Todo, Doing, Done }

enum class Role { Editor, Viewer }

enum class Direction { Left, Right }

data class Card(override val id: CardId, val board: BoardId, val column: Column, val text: CardText) : Identified<CardId>

data class Member(override val id: MemberId, val board: BoardId, val viewer: ViewerId, val role: Role) : Identified<MemberId>

data class Team(val createdAt: Instant, override val id: String = SELF) : Identified<String> {
    companion object {
        const val SELF = "self"
    }
}

data class BoardModel(val cards: List<Card>, val roles: Map<ViewerId, Role>)

data class ViewersModel(val here: Set<ViewerId>, val roles: Map<ViewerId, Role>)

data class CardMove(val board: BoardId, val card: CardId, val direction: Direction)

data class RoleChange(val board: BoardId, val member: ViewerId, val role: Role)

fun cardText(raw: String) = FieldName.of("text").check(CardText, raw, "Card text can't be blank or longer than 100 characters")