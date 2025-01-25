package org.http4k.routing.inspect

// FIXME
//@ExtendWith(ApprovalTest::class)
//class RoutePrintingTest {
//
//    private val routes = routes(
//        "/a" bind Method.GET to { Response(Status.OK).body("matched a") },
//        "/b/c" bind routes(
//            "/d" bind Method.GET to { Response(Status.OK).body("matched b/c/d") },
//            "/e" bind routes(
//                "/f" bind Method.GET to { Response(Status.OK).body("matched b/c/e/f") },
//                "/g" bind routes(
//                    Method.GET to { _: Request -> Response(Status.OK).body("matched b/c/e/g/GET") },
//                    Method.POST to { _: Request -> Response(Status.OK).body("matched b/c/e/g/POST") }
//                )
//            ),
//            "/" bind Method.GET to { Response(Status.OK).body("matched b/c") }
//        )
//    )
//
//    @Test
//    fun `describe routes`(approvalTest: Approver) {
//        routes.description.let {
//            inIntelliJOnly { println(it.prettify()) }
//            approvalTest.assertApproved(it.prettify(escape = Pseudo))
//        }
//    }
//
//    @Test
//    fun `describe matching`(approvalTest: Approver) {
//        val request = Request(Method.POST, "/b/c/e/g")
//
//        routes.match(request).let {
//            inIntelliJOnly { println(it.prettify()) }
//            approvalTest.assertApproved(it.prettify(escape = Pseudo))
//        }
//    }
//}
