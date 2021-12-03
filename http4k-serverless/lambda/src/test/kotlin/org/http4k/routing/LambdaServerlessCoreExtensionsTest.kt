package org.http4k.routing

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.serverless.AwsEnvironment.AWS_LAMBDA_FUNCTION_NAME
import org.http4k.serverless.FnHandler
import org.http4k.serverless.FnLoader
import org.http4k.util.proxy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalStateException

class LambdaServerlessCoreExtensionsTest {

    @Test
    fun `can compose many functions into a single function and routes function name according to environment`() {
        val functions = functions(
            "function1" bind FnLoader {
                FnHandler { _: ScheduledEvent, _: Context ->
                    "function1"
                }
            },
            "func.*1" bind FnLoader {
                FnHandler { _: ScheduledEvent, _: Context ->
                    "func.*1"
                }
            },
            "function2" bind FnLoader {
                FnHandler { _: ScheduledEvent, _: Context ->
                    "function2"
                }
            }
        )

        assertMatch(functions, "function1", "function1")
        assertMatch(functions, "funcy1", "func.*1")
        assertMatch(functions, "function2", "function2")

        assertThrows<IllegalStateException> {
            functions(mapOf(AWS_LAMBDA_FUNCTION_NAME to "foobar"))("{}".byteInputStream(), proxy())
        }
    }

    private fun assertMatch(functions: FnLoader<Context>, name: String, matched: String) {
        assertThat(
            functions(mapOf(AWS_LAMBDA_FUNCTION_NAME to name))("{}".byteInputStream(), proxy()).reader()
                .readText(),
            equalTo(matched)
        )
    }
}
