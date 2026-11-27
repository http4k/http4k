package sl4k

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.state.reading
import org.http4k.fanbelt.testing.TestTenant
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

        chat.enter(tenant.scope, Viewer(ann), ChannelName.of("nowhere")).close()

        tenant {
            assertThat(chat.marks.read(ann), equalTo(emptyMap()))
        }
    }

    @Test
    fun `a teammate visiting a channel leaves your sidebar alone`() {
        val tenant = TestTenant()
        val chat = ChatFeature(tenant.runtime)
        val ann = Viewer(viewerNamed("ann"))
        val bob = Viewer(viewerNamed("bob"))
        val general = ChannelName.of("general")
        Teams(tenant.storage).create(tenant.id, tenant.clock, general, ChannelName.of("random"))
        chat.enter(tenant.scope, ann, general).close()
        chat.enter(tenant.scope, bob, general).close()
        var touched = false
        tenant.scope.bus.subscribe(tenant.scope.reading { ctx -> ctx.read(chat.sidebar, ann.id) }.topics) { touched = true }

        chat.enter(tenant.scope, bob, ChannelName.of("random")).close()
        tenant.settle()

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
        val visit = chat.enter(tenant.scope, ann, general)

        tenant {
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
        val visit = chat.enter(tenant.scope, ann, general)
        tenant {
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
