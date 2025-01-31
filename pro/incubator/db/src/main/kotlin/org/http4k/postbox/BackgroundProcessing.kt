package org.http4k.postbox

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.db.Transactor


fun ProcessPendingRequests(
    transactor: Transactor<Postbox>,
    finalServer: HttpHandler
) = transactor.perform { postbox ->
    // TODO: define a limit for the number of requests to process
    // TODO: exclude requests marked as permanent failures
    // TODO: mark requests as "processing" to allow for multiple instances of this function to run concurrently?
    for (pending in postbox.pendingRequests()) {
        ProcessPendindRequest(postbox, pending, finalServer)
    }
}

fun ProcessPendindRequest(
    postbox: Postbox,
    pending: Postbox.PendingRequest,
    targetHandler: HttpHandler,
    successCriteria: (Response) -> Boolean = { it.status.successful }
): Result<Unit, PostboxError> = targetHandler(pending.request).let { response ->
    if (successCriteria(response)) {
        postbox.markProcessed(pending.requestId, response)
    } else {
        Failure(PostboxError.RequestProcessingFailure("response was not successful"))
    }
}

