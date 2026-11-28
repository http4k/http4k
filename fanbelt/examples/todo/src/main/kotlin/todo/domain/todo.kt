package todo.domain

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.UUIDValue
import dev.forkhandles.values.UUIDValueFactory
import dev.forkhandles.values.ValueFactory
import dev.forkhandles.values.and
import dev.forkhandles.values.length
import dev.forkhandles.values.notBlank
import org.http4k.fanbelt.feature.check
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.store.Identified
import java.time.Instant
import java.util.UUID

class TodoId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<TodoId>(::TodoId)
}

class TodoTitle private constructor(value: String) : StringValue(value) {
    companion object : ValueFactory<TodoTitle, String>(::TodoTitle, (1..200).length and notBlank(), String::trim)
}

data class Todo(override val id: TodoId, val title: TodoTitle, val done: Boolean = false) : Identified<TodoId>

class TodoListId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<TodoListId>(::TodoListId)
}

data class TodoList(val createdAt: Instant, override val id: TodoListId = SELF) : Identified<TodoListId> {
    companion object {
        val SELF = TodoListId.of("self")
    }
}

fun todoTitle(raw: String, field: FieldName = FieldName.of("title")) = field.check(TodoTitle, raw, "Title can't be blank or longer than 200 characters")
