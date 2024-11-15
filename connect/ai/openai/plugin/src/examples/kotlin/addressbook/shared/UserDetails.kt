package addressbook.shared

import org.http4k.core.Credentials

data class UserDetails(
    val name: String,
    val credentials: Credentials,
    val address: String
)
