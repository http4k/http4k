package sl4k

import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.cookie.cookie
import org.http4k.events.MetadataEvent
import org.http4k.fanbelt.feature.OperationRefused
import org.http4k.fanbelt.feature.Refusal
import org.http4k.fanbelt.feature.execute
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.TestRuntime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName
import sl4k.domain.Viewer
import sl4k.domain.NewChannel
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

        assertEquals(
            listOf(OperationRefused("post", "visibleChannel", Refusal.Hidden)),
            runtime.events.map { if (it is MetadataEvent) it.event else it }.filterIsInstance<OperationRefused>(),
        )
    }

    @Test
    fun `a stream from someone we can't identify is sent home`() {
        val refused = node.handler.sse!!(Request(GET, "/teams/acme/c/general"))

        assertEquals(SEE_OTHER, refused.status)
        assertEquals("/", refused.headers.toMap()["location"])
    }

    @Test
    fun `a path nothing answers is a plain 404, inside a channel or out of one`() {
        listOf("general", "secret", "nowhere").forEach {
            assertEquals(NOT_FOUND, http(Request(GET, "/teams/acme/c/$it/no/such/thing").cookie(bob)).status)
        }
    }

    @Test
    fun `to someone outside it, a private channel looks like one that doesn't exist`() {
        listOf("secret", "nowhere").forEach {
            assertEquals(NOT_FOUND, http(Request(GET, "/teams/acme/c/$it").cookie(bob)).status)
            assertEquals(NOT_FOUND, http(Request(POST, "/teams/acme/c/$it/typing").cookie(bob)).status)
        }
        val answers = listOf("secret", "nowhere").map { http(datastarPost("c/$it/messages", bob, """{"text":"Hello?"}""")) }
        assertEquals(answers[0].status, answers[1].status)
        assertEquals(answers[0].bodyString(), answers[1].bodyString())
    }
}