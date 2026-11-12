package sl4k

import org.http4k.fanbelt.testing.TestTenant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName
import sl4k.domain.Draft
import sl4k.domain.PAGE
import sl4k.domain.Viewer
import sl4k.domain.Window
import sl4k.feature.ChatFeature
import sl4k.store.Teams

class ReadMarksTest {
    @Test
    fun `visiting a channel that doesn't exist leaves no read mark behind`() {
        val tenant = TestTenant()
        val chat = ChatFeature(tenant.clock, tenant.random)
        val ann = viewerNamed("ann")
        Teams(tenant.infra.storage).create(tenant.scope.tenant, tenant.clock, ChannelName.of("general"))

        chat.enter(tenant.scope, Viewer(ann), ChannelName.of("nowhere")).close()

        assertEquals(emptyMap<ChannelName, Int>(), tenant.read(chat.marks)[ann].orEmpty())
    }

    @Test
    fun `nothing is new to someone already in the channel, until they leave and more arrives`() {
        val tenant = TestTenant()
        val chat = ChatFeature(tenant.clock, tenant.random)
        val ann = Viewer(viewerNamed("ann"))
        val bob = Viewer(viewerNamed("bob"))
        val general = ChannelName.of("general")
        Teams(tenant.infra.storage).create(tenant.scope.tenant, tenant.clock, general)
        val visit = chat.enter(tenant.scope, ann, general)

        tenant.execute(chat.post, bob, Draft(general, "Hello"))
        assertNull(chat.firstUnread(tenant.scope, ann, general))

        visit.close()
        tenant.execute(chat.post, bob, Draft(general, "Still there?"))
        assertEquals(tenant.read(chat.history, Window(general, PAGE))!!.messages.last().id, chat.firstUnread(tenant.scope, ann, general))
    }
}