package org.http4k.connect.amazon.evidently.actions

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.evidently.EvidentlyAction
import org.http4k.connect.amazon.evidently.model.FeatureName
import org.http4k.connect.amazon.evidently.model.ProjectName
import org.http4k.core.Method
import org.http4k.core.Uri

@Http4kConnectAction
data class DeleteFeature(
    val project: ProjectName,
    val name: FeatureName
) : EvidentlyAction<Unit>(Unit::class, method = Method.DELETE), Action<Result<Unit, RemoteFailure>> {
    override fun uri() = Uri.of("/projects/$project/features/$name")
    override fun requestBody() = Unit
}
