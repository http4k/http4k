package addressbook.shared

import org.http4k.core.Credentials

/**
 * Simple user directory for credentials -> UserDetails
 */
class UserDirectory {
    private val store = listOf(
        UserDetails("Sherlock Holmes", Credentials("sherlock", "watson"), "221b Baker St, London"),
        UserDetails("Paddington Bear", Credentials("paddington", "marmalade"), "Waterloo Station, London")
    ).associateBy { UserId.of(it.credentials.user) }

    fun auth(credentials: Credentials) = find(UserId.of(credentials.user))
        ?.takeIf { it.credentials == credentials }

    fun find(user: UserId): UserDetails? = store[user]

    fun all() = store.keys
}

