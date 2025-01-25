package addressbook.shared

import org.http4k.contract.meta
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Moshi.auto

/**
 * Lookup address for a user
 */
fun GetAllUsers(userDirectory: UserDirectory) = "users" meta {
    summary = "Lookup all users"
    returning(OK, usersLens to listOf(UserId.of("alice"), UserId.of("bob")))
} bindContract GET to { _: Request ->
    Response(OK).with(usersLens of userDirectory.all().toList())
}

private val usersLens = Body.auto<List<UserId>>().toLens()
