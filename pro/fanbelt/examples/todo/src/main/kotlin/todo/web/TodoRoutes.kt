package todo.web

import org.http4k.core.Method.POST
import org.http4k.fanbelt.feature.invoke
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.routing.bind
import org.http4k.routing.routes
import todo.feature.TodoFeature
import todo.view.TodoSignals

fun TodoRoutes(todo: TodoFeature, workspace: Workspace) = routes(
    "/todos" bind POST to todo.add(workspace, { signals: TodoSignals, _ -> signals.title }, reset = setOf(TodoSignals::title)),
    "/todos/{id}/toggle" bind POST to todo.toggle(workspace, input = todoId),
    "/todos/{id}/delete" bind POST to todo.delete(workspace, input = todoId),
    "/todos/{id}/rename" bind POST to todo.rename(workspace, { signals: TodoSignals, request -> todoId(request) to signals.edit }),
)