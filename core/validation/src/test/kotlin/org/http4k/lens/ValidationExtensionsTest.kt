package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.values.Problem
import dev.forkhandles.values.allOf
import dev.forkhandles.values.causes
import dev.forkhandles.values.email
import dev.forkhandles.values.min
import dev.forkhandles.values.notBlank
import dev.forkhandles.values.pattern
import dev.forkhandles.values.validated
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNSUPPORTED_MEDIA_TYPE
import org.http4k.core.UriTemplate
import org.http4k.core.then
import org.http4k.filter.CatchValidationFailure
import org.http4k.filter.ServerFilters
import org.http4k.format.Jackson.auto
import org.http4k.routing.RequestWithContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ValidationExtensionsTest {

    @Test
    fun `validates a query parameter`() {
        val lens = Query.required("email").validated(email())

        assertThat(lens(Request(Method.GET, "/?email=bob@example.com")), equalTo("bob@example.com"))

        val failure = assertThrows<LensFailure> { lens(Request(Method.GET, "/?email=not-an-email")) }
        assertThat((failure.target as Problem).message, equalTo("must be a well-formed email address"))
    }

    @Test
    fun `validates a path parameter`() {
        val lens = Path.int().of("id").validated(min(1))

        assertThat(lens(RequestWithContext(Request(Method.GET, "/users/42"), UriTemplate.from("/users/{id}"))), equalTo(42))

        val failure = assertThrows<LensFailure> {
            lens(RequestWithContext(Request(Method.GET, "/users/0"), UriTemplate.from("/users/{id}")))
        }
        assertThat((failure.target as Problem).message, equalTo("must be greater than or equal to 1"))
    }

    @Test
    fun `validates a header parameter`() {
        val lens = Header.required("X-Code").validated(pattern("\\d+"))

        assertThat(lens(Request(Method.GET, "/").header("X-Code", "123")), equalTo("123"))

        val failure = assertThrows<LensFailure> { lens(Request(Method.GET, "/").header("X-Code", "abc")) }
        assertThat((failure.target as Problem).message, equalTo("must match \"\\d+\""))
    }

    @Test
    fun `validates a json body`() {
        val lens = Body.auto<CreateUser>().toLens().validated(
            allOf(
                CreateUser::name validated notBlank(),
                CreateUser::email validated email(),
                CreateUser::age validated min(18)
            )
        )

        assertThat(
            lens(Request(Method.POST, "/").body("""{"name":"Bob","email":"bob@example.com","age":30}""")),
            equalTo(CreateUser("Bob", "bob@example.com", 30))
        )

        val failure = assertThrows<LensFailure> {
            lens(Request(Method.POST, "/").body("""{"name":"","email":"x","age":17}"""))
        }
        assertThat((failure.target as Problem).causes().map { it.constraint }, equalTo(listOf("NotBlank", "Email", "Min")))
    }

    @Test
    fun `filter returns a bad request with the problem message`() {
        val lens = Query.required("email").validated(email())
        val handler: HttpHandler = { request -> Response(OK).body(lens(request)) }
        val app = ServerFilters.CatchValidationFailure().then(handler)

        val response = app(Request(Method.GET, "/?email=not-an-email"))

        assertThat(response.status, equalTo(BAD_REQUEST))
        assertThat(response.bodyString(), equalTo("must be a well-formed email address"))
    }

    @Test
    fun `filter handles a missing query parameter`() {
        val lens = Query.required("email")
        val handler: HttpHandler = { request -> Response(OK).body(lens(request)) }
        val app = ServerFilters.CatchValidationFailure().then(handler)

        val response = app(Request(Method.GET, "/"))

        assertThat(response.status, equalTo(BAD_REQUEST))
        assertThat(response.bodyString(), equalTo("query 'email' is required"))
    }

    @Test
    fun `filter handles an invalid query parameter`() {
        val lens = Query.int().required("age")
        val handler: HttpHandler = { request -> Response(OK).body(lens(request).toString()) }
        val app = ServerFilters.CatchValidationFailure().then(handler)

        val response = app(Request(Method.GET, "/?age=abc"))

        assertThat(response.status, equalTo(BAD_REQUEST))
        assertThat(response.bodyString(), equalTo("query 'age' must be integer"))
    }

    @Test
    fun `filter returns unsupported media type by default`() {
        val lens = Body.string(ContentType.TEXT_PLAIN, contentNegotiation = ContentNegotiation.Strict).toLens()
        val handler: HttpHandler = { request -> Response(OK).body(lens(request)) }
        val app = ServerFilters.CatchValidationFailure().then(handler)

        val response = app(Request(Method.POST, "/").header("Content-Type", "application/json").body("x"))

        assertThat(response.status, equalTo(UNSUPPORTED_MEDIA_TYPE))
    }

    @Test
    fun `filter uses a custom status mapping`() {
        val lens = Query.required("email").validated(email())
        val handler: HttpHandler = { request -> Response(OK).body(lens(request)) }
        val app = ServerFilters.CatchValidationFailure(statusFor = { FORBIDDEN }).then(handler)

        val response = app(Request(Method.GET, "/?email=not-an-email"))

        assertThat(response.status, equalTo(FORBIDDEN))
    }
}

data class CreateUser(val name: String, val email: String, val age: Int)
