package todo.view

import org.http4k.core.Uri
import org.http4k.fanbelt.page.PageShell
import org.http4k.template.ViewModel
import todo.domain.Todo

data class TodoSignals(val title: String = "", val edit: String = "")

data class TodoListView(val todos: List<Todo>) : ViewModel {
    val remaining = todos.count { !it.done }
}

data class TodoPageView(val shell: PageShell) : ViewModel

data class ListLink(val name: String, val href: Uri)

data class ListsPageView(val lists: List<ListLink>, val typed: String, val error: String?) : ViewModel