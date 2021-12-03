@file:Suppress("unused")

package org.http4k.serverless.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod.CONNECT
import com.microsoft.azure.functions.HttpMethod.DELETE
import com.microsoft.azure.functions.HttpMethod.GET
import com.microsoft.azure.functions.HttpMethod.HEAD
import com.microsoft.azure.functions.HttpMethod.OPTIONS
import com.microsoft.azure.functions.HttpMethod.POST
import com.microsoft.azure.functions.HttpMethod.PUT
import com.microsoft.azure.functions.HttpMethod.TRACE
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel.ANONYMOUS
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import org.http4k.client.ServerForClientContract
import org.http4k.serverless.AzureFunction
import java.util.Optional

class TestFunction : AzureFunction(ServerForClientContract) {
    @FunctionName("testFunction")
    override fun handleRequest(
        @HttpTrigger(name = "req",
            methods = [CONNECT, DELETE, GET, HEAD, OPTIONS, POST, PUT, TRACE],
            authLevel = ANONYMOUS)
        req: HttpRequestMessage<Optional<String>>,
        ctx: ExecutionContext): HttpResponseMessage = handle(req, ctx)
}
