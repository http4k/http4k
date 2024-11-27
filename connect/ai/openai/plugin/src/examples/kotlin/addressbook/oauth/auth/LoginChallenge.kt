package addressbook.oauth.auth

import addressbook.shared.UserDirectory
import addressbook.shared.UserId
import org.http4k.connect.openai.auth.oauth.PrincipalChallenge
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.body.form
import org.http4k.core.with
import org.http4k.lens.Header.LOCATION

/**
 * This is responsible for presenting the login challenge to the user and resolving
 * the details of that challenge when posted back.
 */
fun LoginPrincipalChallenge(userDirectory: UserDirectory) = object : PrincipalChallenge<UserId> {
    override val challenge = { _: Request ->
        Response(OK).body(
            """
            <html>
                <form method="POST">
                    <input name="userId" value="sherlock"/><br/>
                    <input name="password" value="watson"/><br/>
                    <button type="submit">Authenticate</button>
                </form>
            </html>
            """.trimIndent()
        )
    }

    override val handleChallenge = Filter { next ->
        {
            when (userDirectory.auth(Credentials(it.form("userId")!!, it.form("password")!!))) {
                null -> Response(SEE_OTHER).with(LOCATION of it.uri)
                else -> next(it)
            }
        }
    }

    override fun invoke(target: Request) = UserId.of(target.form("userId")!!)
}

