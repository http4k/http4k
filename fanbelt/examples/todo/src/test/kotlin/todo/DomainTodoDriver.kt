package todo

import org.http4k.fanbelt.testing.TestTenant
import todo.domain.TodoTitle
import todo.feature.TodoFeature

class DomainTodoDriver : TodoDriver {
    private val tenant = TestTenant()
    private val todo = TodoFeature(tenant.random)

    override fun user(name: String): User = DomainUser()

    private inner class DomainUser : User {
        override fun watches() {
            seen()
        }

        override fun adds(title: String) = tenant.execute(todo.add, title)

        override fun toggles(title: TodoTitle) {
            tenant.execute(todo.toggle, idOf(title))
        }

        override fun renames(title: TodoTitle, to: String) = tenant.execute(todo.rename, idOf(title) to to)

        override fun deletes(title: TodoTitle) {
            tenant.execute(todo.delete, idOf(title))
        }

        override fun sees() = seen()

        private fun idOf(title: TodoTitle) = tenant.read(todo.list).first { it.title == title }.id

        private fun seen(): ListSeen {
            val all = tenant.read(todo.list)
            return ListSeen(all.map { it.title.value to it.done }, all.count { !it.done })
        }
    }
}