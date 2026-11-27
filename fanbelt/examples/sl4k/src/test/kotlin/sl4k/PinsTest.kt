package sl4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.TestTenant
import org.http4k.fanbelt.testing.Verdict
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName
import sl4k.domain.Draft
import sl4k.domain.MessageRef
import sl4k.domain.PAGE
import sl4k.domain.Reply
import sl4k.domain.ThreadKey
import sl4k.domain.Viewer
import sl4k.domain.Window
import sl4k.feature.ChatFeature
import sl4k.store.Teams

class PinsTest {
    @Test
    fun `a reply can't be pinned`() {
        val tenant = TestTenant()
        val chat = ChatFeature(tenant.runtime)
        val ann = Viewer(viewerNamed("ann"))
        val general = ChannelName.of("general")
        Teams(tenant.storage).create(tenant.id, tenant.clock, general)

        tenant {
            chat.post.execute(ann, Draft(general, "Shipped it"))
            val parent = chat.history.read(Window(general, PAGE))!!.messages.single().id
            chat.reply.execute(ann, Reply(general, parent, "Nice"))
            val reply = chat.thread.read(ThreadKey(general, parent))!!.replies.single().id

            assertThat(chat.togglePin.execute(ann, MessageRef(general, reply)), equalTo(Verdict.Rejected("Only channel messages can be pinned")))
        }
    }
}
