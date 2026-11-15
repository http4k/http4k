package board

import board.domain.BoardId
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.InMemoryCluster
import org.http4k.fanbelt.testing.dies
import org.junit.jupiter.api.Test
import java.time.Duration

private val lobby = BoardId.of("lobby")

class BoardMultiNodeTest : BoardContract() {
    private val cluster = InMemoryCluster()
    override val driver = UiBoardDriver(cluster.runtime.scheduler, cluster.storage, List(2) { App(cluster.runtime, cluster.node()) })

    @Test
    fun `viewers on a node that dies disappear for everyone else once their leases expire`() {
        val sam = driver.member("sam")
        val alex = driver.member("alex")
        sam.opens(lobby)
        sam.watches(lobby)
        alex.watches(lobby)

        assertThat(sam.sees(lobby).members.keys, equalTo(setOf(sam.name, alex.name)))

        cluster.nodes[1].dies()
        cluster.tick(Duration.ofSeconds(45))

        assertThat(sam.sees(lobby).members.keys, equalTo(setOf(sam.name)))
    }
}