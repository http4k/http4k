package org.http4k.routing

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.serverless.FnHandler
import org.http4k.serverless.FunctionLoader
import org.http4k.serverless.lambda.AwsEnvironment.AWS_LAMBDA_FUNCTION_NAME
import org.http4k.util.proxy
import org.junit.jupiter.api.Test

class LambdaCoreExtensionsTest {

    @Test
    fun `can compose many functions into a single function and routes according to environment`() {
        val functions = functions(
            "function1" bind FunctionLoader {
                FnHandler { e: ScheduledEvent, _: Context ->
                    println(e)
                    "function1"
                }
            },
            "function2" bind FunctionLoader {
                FnHandler { e: ScheduledEvent, _: Context ->
                    "function2"
                }
            }
        )

        assertThat(
            functions(mapOf(AWS_LAMBDA_FUNCTION_NAME to "function1"))("{}".byteInputStream(), proxy()).reader()
                .readText(),
            equalTo("function1")
        )
    }
}
