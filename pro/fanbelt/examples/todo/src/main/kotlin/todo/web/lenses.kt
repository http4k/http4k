package todo.web

import org.http4k.lens.Path
import org.http4k.lens.value
import todo.domain.TodoId

val todoId = Path.value(TodoId).of("id")