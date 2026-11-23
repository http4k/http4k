package todo.feature

import dev.forkhandles.values.random
import todo.domain.Todo
import todo.domain.TodoId
import todo.domain.TodoTitle
import todo.store.TodoTables
import java.util.Random

class Todos(private val tables: TodoTables, private val random: Random) {
    fun all() = tables.todos.page().items

    fun add(title: TodoTitle) = tables.todos.insert(Todo(TodoId.random(random), title))

    fun toggle(id: TodoId) = tables.todos.update(id) { it.copy(done = !it.done) }

    fun rename(id: TodoId, title: TodoTitle) = tables.todos.update(id) { it.copy(title = title) }

    fun delete(id: TodoId) = tables.todos.delete(id)
}