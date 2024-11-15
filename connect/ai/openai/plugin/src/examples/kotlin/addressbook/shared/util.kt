package addressbook.shared

import org.http4k.config.Environment
import org.http4k.core.Credentials
import org.http4k.lens.BiDiLensSpec

fun BiDiLensSpec<Environment, String>.credentials() = map({
    it.split(":")
        .let { (clientId, clientSecret) -> Credentials(clientId, clientSecret) }
}, {
    it.user + ":" + it.password
})
