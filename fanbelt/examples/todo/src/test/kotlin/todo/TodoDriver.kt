package todo

import org.http4k.fanbelt.testing.Verdict
import todo.domain.TodoTitle

interface TodoDriver {
    fun user(name: String): User
}

interface User {
    fun watches()
    fun adds(title: TodoTitle): Verdict = adds(title.value)
    fun adds(title: String): Verdict
    fun toggles(title: TodoTitle)
    fun renames(title: TodoTitle, to: TodoTitle): Verdict = renames(title, to.value)
    fun renames(title: TodoTitle, to: String): Verdict
    fun deletes(title: TodoTitle)
    fun sees(): ListSeen
}

data class ListSeen(val todos: List<Pair<String, Boolean>>, val remaining: Int)