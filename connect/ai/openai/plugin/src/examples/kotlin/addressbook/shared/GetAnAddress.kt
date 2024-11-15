package addressbook.shared

import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Path
import org.http4k.lens.string
import org.http4k.lens.value

/**
 * Lookup address for a user
 */
fun GetAnAddress(userDirectory: UserDirectory) = "address" / Path.value(UserId).of("user") meta {
    summary = "Lookup my address"
    returning(OK, addressLens to "10 Downing Street, London")
} bindContract GET to { user ->
    {
        when (val userDetails = userDirectory.find(user)) {
            null -> Response(NOT_FOUND)
            else -> Response(OK).with(addressLens of userDetails.address)
        }
    }
}

private val addressLens = Body.string(TEXT_PLAIN).toLens()
