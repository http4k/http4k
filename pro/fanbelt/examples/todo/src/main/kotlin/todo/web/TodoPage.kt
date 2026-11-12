package todo.web

import org.http4k.fanbelt.page.Layout
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.page.Region
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.template.TemplateRenderer
import todo.feature.TodoFeature
import todo.view.TodoListView
import todo.view.TodoPageView
import todo.view.TodoSignals

fun TodoPage(workspace: Workspace, todo: TodoFeature, renderer: TemplateRenderer): Page {
    val list = Region("todos", renderer, todo.list) { todos, _ -> TodoListView(todos) }
    return Page(workspace, Layout(listOf(list), renderer, signals = TodoSignals()) { _, shell -> TodoPageView(shell) })
}