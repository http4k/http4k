package todo.feature

import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.feature.orElse
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.state.topic
import todo.domain.TodoId
import todo.domain.todoTitle
import todo.store.TodoTables
import java.util.Random

class TodoFeature(random: Random) : Feature<Todos, Unit>({ tables -> Todos(TodoTables(tables), random) }) {
    val list by state { it.all() }

    val add by operation { todos, _, raw: String ->
        val title = todoTitle(raw).orElse { return@operation it }
        todos.add(title)
        Changes.of(list.topic())
    }

    val toggle by operation { todos, _, id: TodoId ->
        todos.toggle(id)
        Changes.of(list.topic())
    }

    val rename by operation { todos, _, (id, raw): Pair<TodoId, String> ->
        val title = todoTitle(raw, FieldName.of("edit")).orElse { return@operation it }
        todos.rename(id, title)
        Changes.of(list.topic())
    }

    val delete by operation { todos, _, id: TodoId ->
        todos.delete(id)
        Changes.of(list.topic())
    }
}