package org.http4k.core

/**
 * Mock http handler, captures the request and returns the given response
 */
class MockHttp(val response: Response = Response(Status.OK)) : HttpHandler {
    var request: Request? = null

    override fun invoke(request: Request): Response {
        this.request = request
        return response
    }
}
