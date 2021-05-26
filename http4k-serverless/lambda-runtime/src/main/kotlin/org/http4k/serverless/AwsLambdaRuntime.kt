package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.server.Http4kServer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Controls the function execution loop. Continues to process events to this function
 * until either it encounters an operational error or AWS terminates the runtime.
 */
class AwsLambdaRuntime(
    private val env: Map<String, String> = System.getenv(),
    private val http: HttpHandler = JavaHttpClient(),
) : ServerlessConfig<Context> {
    override fun asServer(fn: FnLoader<Context>) = object : Http4kServer {

        @Suppress("HttpUrlsUsage")
        private val runtime = LambdaRuntimeAPI(
            SetHostFrom(Uri.of("http://${env.getValue("AWS_LAMBDA_RUNTIME_API")}")).then(http)
        )

        private val done = AtomicBoolean(false)

        override fun port() = throw UnsupportedOperationException("not port bound")

        override fun start() = apply {
            val lambda = try {
                fn(env)
            } catch (e: Exception) {
                runtime.initError(e)
                throw e
            }

            thread {
                do {
                    runtime.nextInvocation().use { nextInvocation ->
                        try {
                            lambda(nextInvocation.body.stream, LambdaEnvironmentContext(nextInvocation, env)).use {
                                runtime.success(nextInvocation, it)
                            }
                        } catch (e: Exception) {
                            runtime.error(nextInvocation, e)
                        }
                    }
                } while (!done.get())
            }
        }

        override fun stop() = apply { done.set(true) }
    }
}
