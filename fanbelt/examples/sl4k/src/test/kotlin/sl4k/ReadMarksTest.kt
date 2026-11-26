package sl4k

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.state.reading
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.TestTenant
import org.http4k.fanbelt.testing.infrastructure.TestRuntime
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName
import sl4k.domain.Draft
import sl4k.domain.PAGE
import sl4k.domain.Reader
import sl4k.domain.Seen
import sl4k.domain.Viewer
import sl4k.domain.Window
import sl4k.feature.ChatFeature
import sl4k.store.Teams

class ReadMarksTest {
    @Test
    fun `visiting a channel that doesn't exist leaves no read mark behind`() {
        val tenant = TestTenant()
        val chat = ChatFeature(tenant.runtime)
        val ann = viewerNamed("ann")
        Teams(tenant.storage).create(tenant.id, tenant.clock, ChannelName.of("general"))

        tenant {
            chat.enter.open(Viewer(ann), ChannelName.of("nowhere")).close()

            assertThat(chat.marks.read(ann), equalTo(emptyMap()))
        }
    }

    @Test
    fun `a teammate visiting a channel leaves your sidebar alone`() {
        val runtime = TestRuntime()
        val storage = InMemoryDocumentStorage()
        val scope = AppInfra(runtime, storage).apply { start() }.scope(TenantId.of("acme"))
        val chat = ChatFeature(runtime)
        val ann = Viewer(viewerNamed("ann"))
        val bob = Viewer(viewerNamed("bob"))
        val general = ChannelName.of("general")
        Teams(storage).create(scope.tenant, runtime.clock, general, ChannelName.of("random"))
        chat.enter.open(scope, ann, general).close()
        chat.enter.open(scope, bob, general).close()
        runtime.scheduler.runUntilIdle()
        var touched = false
        scope.bus.subscribe(scope.reading { ctx -> ctx.read(chat.sidebar, ann.id) }.topics) { touched = true }

        chat.enter.open(scope, bob, ChannelName.of("random")).close()
        runtime.scheduler.runUntilIdle()

        assertThat(touched, equalTo(false))
    }

    @Test
    fun `nothing is new to someone already in the channel, until they leave and more arrives`() {
        val tenant = TestTenant()
        val chat = ChatFeature(tenant.runtime)
        val ann = Viewer(viewerNamed("ann"))
        val bob = Viewer(viewerNamed("bob"))
        val general = ChannelName.of("general")
        Teams(tenant.storage).create(tenant.id, tenant.clock, general)
        tenant {
            val visit = chat.enter.open(ann, general)

            chat.post.execute(bob, Draft(general, "Hello"))
            assertThat(chat.firstUnread.read(Reader(general, ann.id)), absent())

            chat.read.execute(ann, Seen(general, 1))
            visit.close()
            chat.post.execute(bob, Draft(general, "Still there?"))
            assertThat(chat.firstUnread.read(Reader(general, ann.id)), equalTo(chat.history.read(Window(general, PAGE))!!.messages.last().id))
        }
    }

    @Test
    fun `what arrives after your page last showed the channel stays unread, even before your leaving is noticed`() {
        val tenant = TestTenant()
        val chat = ChatFeature(tenant.runtime)
        val ann = Viewer(viewerNamed("ann"))
        val bob = Viewer(viewerNamed("bob"))
        val general = ChannelName.of("general")
        Teams(tenant.storage).create(tenant.id, tenant.clock, general)
        tenant {
            val visit = chat.enter.open(ann, general)
            chat.post.execute(bob, Draft(general, "Hello"))
            chat.read.execute(ann, Seen(general, 1))

            chat.post.execute(bob, Draft(general, "Still there?"))
            visit.close()

            assertThat(chat.firstUnread.read(Reader(general, ann.id)), equalTo(chat.history.read(Window(general, PAGE))!!.messages.last().id))
        }
    }

    @Test
    fun `an older page saying it showed less doesn't bring back what you have read`() {
        val tenant = TestTenant()
        val chat = ChatFeature(tenant.runtime)
        val ann = Viewer(viewerNamed("ann"))
        val bob = Viewer(viewerNamed("bob"))
        val general = ChannelName.of("general")
        Teams(tenant.storage).create(tenant.id, tenant.clock, general)
        tenant {
            chat.post.execute(bob, Draft(general, "Hello"))
            chat.post.execute(bob, Draft(general, "Still there?"))
            chat.read.execute(ann, Seen(general, 2))

            chat.read.execute(ann, Seen(general, 1))

            assertThat(chat.firstUnread.read(Reader(general, ann.id)), absent())
        }
    }
}
