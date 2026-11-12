package sl4k

import org.http4k.fanbelt.testing.TestTenant
import org.http4k.fanbelt.testing.Verdict
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName
import sl4k.domain.Draft
import sl4k.domain.Viewer
import sl4k.domain.MessageRef
import sl4k.domain.PAGE
import sl4k.domain.Reply
import sl4k.domain.ThreadKey
import sl4k.domain.Window
import sl4k.feature.ChatFeature
import sl4k.store.Teams

class PinsTest {
    @Test
    fun `a reply can't be pinned`() {
        val tenant = TestTenant()
        val chat = ChatFeature(tenant.clock, tenant.random)
        val ann = Viewer(viewerNamed("ann"))
        val general = ChannelName.of("general")
        Teams(tenant.infra.storage).create(tenant.scope.tenant, tenant.clock, general)
        tenant.execute(chat.post, ann, Draft(general, "Shipped it"))
        val parent = tenant.read(chat.history, Window(general, PAGE))!!.messages.single().id
        tenant.execute(chat.reply, ann, Reply(general, parent, "Nice"))
        val reply = tenant.read(chat.thread, ThreadKey(general, parent))!!.replies.single().id

        assertEquals(Verdict.Rejected("Only channel messages can be pinned"), tenant.execute(chat.togglePin, ann, MessageRef(general, reply)))
    }
}
