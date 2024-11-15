package addressbook.shared

import org.http4k.contract.meta
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.string

/**
 * Lookup address for the principal, which has been stored during the auth process
 */
fun GetMyAddress(
    userDirectory: UserDirectory,
    principal: (Request) -> UserId?
) = "address" meta {
    summary = "Lookup my address"
    returning(OK, addressLens to "10 Downing Street, London")
} bindContract GET to { req: Request ->
    when (val userDetails = principal(req)?.let { userDirectory.find(it) }) {
        null -> Response(NOT_FOUND)
        else -> Response(OK).with(addressLens of userDetails.address)
    }
}

private val addressLens = Body.string(TEXT_PLAIN).toLens()

