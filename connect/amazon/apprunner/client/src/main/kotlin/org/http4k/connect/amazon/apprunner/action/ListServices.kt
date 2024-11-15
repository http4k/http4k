package org.http4k.connect.amazon.apprunner.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.Paged
import org.http4k.connect.PagedAction
import org.http4k.connect.amazon.apprunner.AppRunnerAction
import org.http4k.connect.amazon.apprunner.model.NextToken
import org.http4k.connect.amazon.apprunner.model.ServiceId
import org.http4k.connect.amazon.apprunner.model.ServiceName
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.kClass
import org.http4k.connect.model.Timestamp
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class ListServices(val NextToken: NextToken? = null) : AppRunnerAction<ServiceSummaryList>(kClass()),
    PagedAction<NextToken, ServiceSummary, ServiceSummaryList, ListServices> {
    override fun next(token: NextToken) = copy(NextToken = token)
}

@JsonSerializable
data class ServiceSummary(
    val CreatedAt: Timestamp,
    val ServiceArn: ARN,
    val ServiceId: ServiceId,
    val ServiceName: ServiceName,
    val ServiceUrl: Uri?,
    val Status: String?,
    val UpdatedAt: Timestamp
)

@JsonSerializable
data class ServiceSummaryList(val ServiceSummaryList: List<ServiceSummary>, val NextToken: NextToken? = null) :
    Paged<NextToken, ServiceSummary> {
    override fun token() = NextToken

    override val items get() = ServiceSummaryList
}
