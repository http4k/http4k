package board

import board.domain.BoardId
import org.http4k.fanbelt.testing.InMemoryCluster
import org.http4k.fanbelt.testing.dies
import org.junit.jupiter.api.Assertions.assertEquals
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

        assertEquals(setOf(sam.name, alex.name), sam.sees(lobby).members.keys)

        cluster.nodes[1].dies()
        cluster.tick(Duration.ofSeconds(45))

        assertEquals(setOf(sam.name), sam.sees(lobby).members.keys)
    }
}