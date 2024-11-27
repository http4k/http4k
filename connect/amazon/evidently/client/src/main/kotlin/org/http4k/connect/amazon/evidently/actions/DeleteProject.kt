package org.http4k.connect.amazon.evidently.actions

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.evidently.EvidentlyAction
import org.http4k.connect.amazon.evidently.model.ProjectName
import org.http4k.core.Method
import org.http4k.core.Uri

@Http4kConnectAction
data class DeleteProject(
    val name: ProjectName
) : EvidentlyAction<Unit>(Unit::class, method = Method.DELETE) {
    override fun uri() = Uri.of("/projects/$name")
    override fun requestBody() = Unit
}
