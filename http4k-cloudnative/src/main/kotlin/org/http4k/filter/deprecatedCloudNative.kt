package org.http4k.filter

import org.http4k.cloudnative.RemoteRequestFailed
import org.http4k.core.Response

@Deprecated("Renamed", ReplaceWith("ServerFilters.HandleRemoteRequestFailed"))
fun ServerFilters.HandleUpstreamRequestFailed(
    exceptionToBody: RemoteRequestFailed.() -> String = { localizedMessage }
) = HandleRemoteRequestFailed(exceptionToBody)

@Deprecated("Renamed", ReplaceWith("ClientFilters.HandleRemoteRequestFailed"))
fun ClientFilters.HandleUpstreamRequestFailed(
    responseWasSuccessful: Response.() -> Boolean = { status.successful },
    responseToMessage: Response.() -> String = Response::bodyString
) = HandleRemoteRequestFailed(responseWasSuccessful, responseToMessage)
