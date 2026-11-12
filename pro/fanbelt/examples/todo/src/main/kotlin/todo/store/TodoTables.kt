package todo.store

import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.invoke
import todo.domain.Todo
import todo.domain.TodoId
import todo.domain.TodoList

class TodoTables(tables: Tables) {
    val todos = tables<TodoId, Todo>(TODOS)
    val list = tables<String, TodoList>(LISTS)

    companion object {
        val TODOS = TableName.of("todos")
        val LISTS = TableName.of("list")
    }
}