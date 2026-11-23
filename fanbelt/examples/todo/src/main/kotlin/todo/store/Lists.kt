package todo.store

import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.everything
import org.http4k.fanbelt.tenancy.TenantId
import todo.domain.TodoList

class Lists(private val storage: DocumentStorage) {
    fun all(): List<TenantId> = storage.read { it.having(TodoTables.LISTS, everything<TodoList>()) }.sortedBy { it.value }

    fun exists(list: TenantId) = storage.read { TodoTables(it.tables(list)).list.get(TodoList.SELF) != null }

    fun create(list: TenantId, record: TodoList) = storage.write { tenants ->
        val lists = TodoTables(tenants.tables(list)).list
        if (lists.get(TodoList.SELF) != null) false
        else {
            lists.insert(record)
            true
        }
    }
}