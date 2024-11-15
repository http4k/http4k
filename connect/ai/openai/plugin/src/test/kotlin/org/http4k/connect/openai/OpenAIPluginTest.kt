package org.http4k.connect.openai

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.openai.auth.PluginAuthToken
import org.http4k.connect.openai.auth.user.UserLevelAuth
import org.http4k.connect.openai.model.Email
import org.http4k.connect.openai.testing.OpenApiPluginRequirements
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.Body
import org.http4k.core.Credentials
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters.BasicAuth
import org.http4k.filter.debug
import org.http4k.lens.Path
import org.http4k.security.oauth.format.OAuthMoshi.auto
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test

class OpenAIPluginTest : OpenApiPluginRequirements {

    private val credentials = Credentials("foo", "bar")
    override val openAiPlugin = openAiPlugin(
        info(
            apiVersion = "1.0",
            humanDescription = "addressbook" to "my great plugin",
            pluginUrl = Uri.of("http://localhost:9000"),
            contactEmail = Email.of("foo@bar"),
        ),
        UserLevelAuth(
            PluginAuthToken.Basic("realm") { it: Credentials -> it == credentials }
        ),
        Path.of("foo") / Path.of("bar") meta {
            summary = "A great api endpoint"
        } bindContract GET to
            { foo, bar ->
                { _: Request -> Response(OK).with(Body.auto<Message>().toLens() of Message("hello $foo $bar")) }
            }
    ).debug()

    data class Message(val message: String)

    @Test
    fun `endpoint is routed to`(approver: Approver) {
        approver.assertApproved(
            BasicAuth(credentials).then(openAiPlugin)(Request(GET, "/foo/bar"))
        )
    }

    @Test
    fun `applies security to routes`() {
        assertThat(
            openAiPlugin(Request(GET, "/foo/bar")).status,
            equalTo(UNAUTHORIZED)
        )
    }
}
