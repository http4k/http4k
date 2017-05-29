package org.http4k.routing

//class RoutedHandlerTest {
//
//    @Test
//    fun not_found() {
//        val routes = routes()
//
//        val response = routes(Request(Method.GET, "/a/something"))
//
//        assertThat(response.status, equalTo(NOT_FOUND))
//        assertThat(response.status.description, equalTo("Route not found"))
//    }
//
//    @Test
//    fun method_not_allowed() {
//        val routes = routes(
//            GET to "/a/{route}" by { _: Request -> Response(Status.OK).body("matched") }
//        )
//
//        val response = routes(Request(Method.POST, "/a/something"))
//
//        assertThat(response.status, equalTo(METHOD_NOT_ALLOWED))
//    }
//
//    @Test
//    fun matches_uri_template_and_method() {
//        val routes = routes(
//            GET to "/a/{route}" by { _: Request -> Response(Status.OK).body("matched") }
//        )
//
//        val response = routes(Request(Method.GET, "/a/something"))
//
//        assertThat(response.bodyString(), equalTo("matched"))
//    }
//
//    @Test
//    fun matches_uses_first_match() {
//        val routes = routes(
//            GET to "/a/{route}" by { _: Request -> Response(Status.OK).body("matched a") },
//            GET to "/a/{route}" by { _: Request -> Response(Status.OK).body("matched b") }
//        )
//
//        val response = routes(Request(Method.GET, "/a/something"))
//
//        assertThat(response.bodyString(), equalTo("matched a"))
//    }
//
//    @Test
//    fun path_parameters_are_available_in_request() {
//        val routes = routes(
//            GET to "/{a}/{b}/{c}" by { req: Request -> Response(Status.OK).body("matched ${req.path("a")}, ${req.path("b")}, ${req.path("c")}") }
//        )
//
//        val response = routes(Request(Method.GET, "/x/y/z"))
//
//        assertThat(response.bodyString(), equalTo("matched x, y, z"))
//    }
//
//    @Test
//    fun matches_uri_with_query() {
//        val routes = routes(GET to "/a/b" by { Response(Status.OK) })
//
//        val response = routes(Request(Method.GET, "/a/b?foo=bar"))
//
//        assertThat(response, equalTo(Response(Status.OK)))
//    }
//
//    @Test
//    fun matches_request_with_extra_path_parts() {
//        val routes = routes(GET to "/a" by { Response(Status.OK) })
//
//        val response = routes(Request(Method.GET, "/a/b"))
//
//        assertThat(response, equalTo(Response(Status.OK)))
//    }
//
//    @Test
//    fun can_stop_matching_extra_parts() {
//        val routes = routes(GET to "/a{$}" by { Response(Status.OK) })
//
//        val response = routes(Request(Method.GET, "/a/b"))
//
//        assertThat(response, equalTo(Response(NOT_FOUND)))
//    }
//
//    @Test
//    fun breaks_if_trying_to_access_path_parameters_without_header_present() {
//        try {
//            Request(Method.GET, "/").path("abc")
//            fail("Expected exception")
//        } catch (e: IllegalStateException) {
//            assertThat(e.message, equalTo("x-uri-template header not present in the request"))
//        }
//    }
//}