package todo.web

import org.http4k.core.Method.POST
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.routing.bind
import org.http4k.routing.routes
import todo.feature.TodoFeature
import todo.view.TodoSignals

fun TodoRoutes(todo: TodoFeature, workspace: Workspace) = routes(
    "/todos" bind POST to workspace.execute(todo.add, reset = setOf(TodoSignals::title)) { signals: TodoSignals -> signals.title },
    "/todos/{id}/toggle" bind POST to workspace.execute(todo.toggle, input = todoId),
    "/todos/{id}/delete" bind POST to workspace.execute(todo.delete, input = todoId),
    "/todos/{id}/rename" bind POST to workspace.execute(todo.rename, todoId) { signals: TodoSignals, id -> id to signals.edit },
)