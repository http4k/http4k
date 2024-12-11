package org.http4k.routing

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.BiDiPathLens
import org.http4k.lens.Path
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class RouterDescriptionTest {

    @Test
    fun `toString is friendly`(approver: Approver) {
        val and = GET.and(headers("host"))

        approver.assertApproved(and.and(headers("host")).toString())
    }

    @Test
    fun `complicated toString`(approver: Approver) {
        val routes = reverseProxyRouting(
            "host" to routes("/foo" bind GET to { Response(OK) }),
            "anotherHost" to routes("/bar" bind GET to { Response(OK) }
            )
        )

        approver.assertApproved(routes.toString())
    }

    @Test
    fun `complicated toString with description`(approver: Approver) {
        val handler: HttpHandler = { Response(OK) }

        val template = routes(
            "/foo/{any:.*}" bind GET to handler,
            "/bar" bind handler
        )

        val reverseProxy = routes(
            "proxy" bind reverseProxyRouting(
                "hostA" to template,
                "hostB" to template
            )
        )

        val spa = routes(
            "/spa" bind routes(
                "/directory" bind singlePageApp(ResourceLoader.Directory("/tmp")),
                "/classpath" bind singlePageApp(ResourceLoader.Classpath()), singlePageApp()
            )
        )

        val static = routes(
            "/static" bind routes(
                "/directory" bind static(ResourceLoader.Directory("/tmp")),
                "/classpath" bind static(ResourceLoader.Classpath()),
            )
        )

        // TODO include predicate description
        @Suppress("UNUSED_VARIABLE")
        val predicates = routes(
            "/predicates" bind routes(
                query("q", "foo")
                    .or(headers("q").and(header("q", "foo")))
                    .or(body({ b: String -> b == "foo" })) bind handler,
            )
        )

        val routes = routes(
            reverseProxy,
            static,
            spa,
//            predicates,
            orElse bind handler
        )

        approver.assertApproved(routes.toString())
    }
}
