package org.http4k.specmatic

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import `in`.specmatic.core.HttpRequest
import `in`.specmatic.core.HttpResponse
import `in`.specmatic.core.NamedStub
import `in`.specmatic.core.value.StringValue
import `in`.specmatic.mock.ScenarioStub
import org.http4k.core.ContentType.Companion.APPLICATION_YAML
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.testing.Approver
import org.http4k.testing.YamlApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString

@ExtendWith(YamlApprovalTest::class)
class DirectoryInteractionStorageTest {

    @Test
    fun `stores traffic`(approver: Approver) {
        val dir = createTempDirectory()
        DirectoryInteractionStorage(dir.pathString).store(
            listOf(
                NamedStub(
                    "foo", ScenarioStub(
                        HttpRequest(
                            "GET", "/path",
                            mapOf("h1" to "v1"),
                            StringValue("body"),
                            mapOf("q1" to "v1"),
                            mapOf("f1" to "v1")
                        ),
                        HttpResponse(100, "foobar", mapOf("h1" to "v1")),
                    )
                )
            )
        )

        approver.assertApproved(
            Response(OK)
                .with(CONTENT_TYPE of APPLICATION_YAML)
                .body(
                File(dir.toFile(), "proxy_generated.yaml").readText()
            )
        )

        val scenarioDir = File(dir.toFile(), "proxy_generated_data")
        assertThat(
            File(scenarioDir, "stub0.json").readText(), equalTo(
                """
{
    "http-request": {
        "path": "/path",
        "method": "GET",
        "query": {
            "q1": "v1"
        },
        "headers": {
            "h1": "v1"
        },
        "form-fields": {
            "f1": "v1"
        }
    },
    "http-response": {
        "status": 100,
        "body": "foobar",
        "status-text": "Continue",
        "headers": {
            "h1": "v1"
        }
    }
}""".trimIndent()
            )
        )
    }
}
