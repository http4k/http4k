package addressbook.user

import addressbook.shared.UserDirectory
import addressbook.shared.UserId
import org.http4k.connect.openai.auth.PluginAuthToken
import org.http4k.lens.RequestContextLens

/**
 * Populate a known user if their password matches
 */
fun UserDirectory.authUser(userPrincipal: RequestContextLens<UserId>) =
    PluginAuthToken.Basic("realm", userPrincipal) { credentials ->
        auth(credentials)
            ?.credentials?.user
            ?.let(UserId::of)
    }
