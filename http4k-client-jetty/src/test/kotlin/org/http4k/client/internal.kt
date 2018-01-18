package org.http4k.client

import org.eclipse.jetty.client.api.Request
import java.util.concurrent.TimeUnit

internal val timeout = { request: Request -> request.timeout(100, TimeUnit.MILLISECONDS) }