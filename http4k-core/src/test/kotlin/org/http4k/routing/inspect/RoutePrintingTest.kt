package org.http4k.routing.inspect

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.inspect.EscapeMode.Pseudo
import org.http4k.routing.routes
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.http4k.util.inIntelliJOnly
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith


@ExtendWith(ApprovalTest::class)
class RoutePrintingTest {

    private val routes = routes(
        "/a" bind Method.GET to { Response(Status.OK).body("matched a") },
        "/b/c" bind routes(
            "/d" bind Method.GET to { Response(Status.OK).body("matched b/c/d") },
            "/e" bind routes(
                "/f" bind Method.GET to { Response(Status.OK).body("matched b/c/e/f") },
                "/g" bind routes(
                    Method.GET to { _: Request -> Response(Status.OK).body("matched b/c/e/g/GET") },
                    Method.POST to { _: Request -> Response(Status.OK).body("matched b/c/e/g/POST") }
                )
            ),
            "/" bind Method.GET to { Response(Status.OK).body("matched b/c") }
        )
    )

    @Test
    fun `describe routes`(approvalTest: Approver) {
        routes.description.let {
            inIntelliJOnly { println(it.prettify()) }
            approvalTest.assertApproved(it.prettify(escapeMode = Pseudo))
        }
    }

    @Test
    fun `describe matching`(approvalTest: Approver) {
        val request = Request(Method.POST, "/b/c/e/g")

        routes.match(request).let {
            inIntelliJOnly { println(it.prettify()) }
            approvalTest.assertApproved(it.prettify(escapeMode = Pseudo))
        }
    }
}


