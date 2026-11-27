package sl4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.TestTenant
import org.http4k.fanbelt.testing.Verdict
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName
import sl4k.domain.Draft
import sl4k.domain.PAGE
import sl4k.domain.React
import sl4k.domain.Viewer
import sl4k.domain.Window
import sl4k.feature.ChatFeature
import sl4k.store.Teams

class ReactionsTest {
    @Test
    fun `an emoji outside the palette is refused`() {
        val tenant = TestTenant()
        val chat = ChatFeature(tenant.runtime)
        val ann = Viewer(viewerNamed("ann"))
        Teams(tenant.storage).create(tenant.id, tenant.clock, ChannelName.of("general"))
        tenant {
            chat.post.execute(ann, Draft(ChannelName.of("general"), "Shipped it"))
            val message = chat.history.read(Window(ChannelName.of("general"), PAGE))!!.messages.single().id

            assertThat(chat.react.execute(ann, React(ChannelName.of("general"), message, "pizza")), equalTo(Verdict.Rejected("No such reaction")))
        }
    }
}