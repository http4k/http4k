package sl4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.cookie.cookie
import org.http4k.events.MetadataEvent
import org.http4k.fanbelt.feature.OperationRefused
import org.http4k.fanbelt.feature.Outcome
import org.http4k.fanbelt.feature.execute
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.TestRuntime
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName
import sl4k.domain.NewChannel
import sl4k.domain.Viewer
import sl4k.feature.ChatFeature
import sl4k.store.Teams
import sl4k.web.viewerCookie

class ChannelMembersTest {
    private val storage = InMemoryDocumentStorage()
    private val runtime = TestRuntime()
    private val node = App(runtime) { AppInfra(it, storage) }.apply { start() }
    private val http = node.handler.http!!
    private val bob = viewerCookie(viewerNamed("bob"))

    init {
        val acme = TenantId.of("acme")
        Teams(storage).create(acme, runtime.clock, ChannelName.of("general"))
        AppInfra(runtime, storage).apply { start() }.scope(acme).execute(ChatFeature(runtime.clock, runtime.random).create, Viewer(viewerNamed("ann")), NewChannel("secret", private = true))
    }

    @Test
    fun `someone knocking on a private channel they can't see is on the record`() {
        http(datastarPost("c/secret/messages", bob, """{"text":"Hello?"}"""))

        assertThat(runtime.events.map { if (it is MetadataEvent) it.event else it }.filterIsInstance<OperationRefused>(), equalTo(listOf(OperationRefused("post", "visibleChannel", Outcome.Hidden))))
    }

    @Test
    fun `someone who can't see a channel, or asks for one that doesn't exist, is sent to be told there's no such channel`() {
        listOf("secret", "nowhere").forEach {
            assertThat(http(Request(GET, "/teams/acme/c/$it").cookie(bob)).bodyString(), containsSubstring("""window.location.assign("/teams/acme/no-such-channel")"""))
        }
        val told = http(Request(GET, "/teams/acme/no-such-channel").cookie(bob))
        assertThat(told.status, equalTo(NOT_FOUND))
        assertThat(told.bodyString(), containsSubstring("There's no such channel"))
    }

    @Test
    fun `a stream from someone we can't identify is sent home`() {
        val refused = node.handler.sse!!(Request(GET, "/teams/acme/c/general"))

        assertThat(refused.status, equalTo(SEE_OTHER))
        assertThat(refused.headers.toMap()["location"], equalTo("/"))
    }

    @Test
    fun `a path nothing answers is a plain 404, inside a channel or out of one`() {
        listOf("general", "secret", "nowhere").forEach {
            assertThat(http(Request(GET, "/teams/acme/c/$it/no/such/thing").cookie(bob)).status, equalTo(NOT_FOUND))
        }
    }

    @Test
    fun `to someone outside it, a private channel looks like one that doesn't exist`() {
        listOf("secret", "nowhere").forEach {
            assertThat(http(Request(GET, "/teams/acme/c/$it").cookie(bob)).status, equalTo(NOT_FOUND))
            assertThat(http(Request(POST, "/teams/acme/c/$it/typing").cookie(bob)).status, equalTo(NOT_FOUND))
        }
        val answers = listOf("secret", "nowhere").map { http(datastarPost("c/$it/messages", bob, """{"text":"Hello?"}""")) }
        assertThat(answers[1].status, equalTo(answers[0].status))
        assertThat(answers[1].bodyString(), equalTo(answers[0].bodyString()))
    }
}