package sl4k

import org.http4k.fanbelt.testing.TestTenant
import org.http4k.fanbelt.testing.Verdict
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName
import sl4k.domain.Draft
import sl4k.domain.Viewer
import sl4k.domain.PAGE
import sl4k.domain.React
import sl4k.domain.Window
import sl4k.feature.ChatFeature
import sl4k.store.Teams

class ReactionsTest {
    @Test
    fun `an emoji outside the palette is refused`() {
        val tenant = TestTenant()
        val chat = ChatFeature(tenant.clock, tenant.random)
        val ann = Viewer(viewerNamed("ann"))
        Teams(tenant.infra.storage).create(tenant.scope.tenant, tenant.clock, ChannelName.of("general"))
        tenant.execute(chat.post, ann, Draft(ChannelName.of("general"), "Shipped it"))
        val message = tenant.read(chat.history, Window(ChannelName.of("general"), PAGE))!!.messages.single().id

        assertEquals(Verdict.Rejected("No such reaction"), tenant.execute(chat.react, ann, React(ChannelName.of("general"), message, "pizza")))
    }
}