package boxoffice

import boxoffice.domain.Actor
import boxoffice.domain.Layout
import boxoffice.domain.SeatRef
import boxoffice.domain.Shopper
import boxoffice.store.BoxOfficeTables
import boxoffice.store.Theatres
import dev.forkhandles.time.DeterministicScheduler
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.HtmlMarbles
import org.http4k.fanbelt.testing.MarbleEvents
import org.http4k.fanbelt.testing.TestRuntime
import org.http4k.fanbelt.testing.start
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.Random

@ExtendWith(ApprovalTest::class)
class BoxOfficeMarblesTest {
    @RegisterExtension
    val marbles = MarbleEvents()

    private val scheduler = DeterministicScheduler(start)
    private val storage = InMemoryDocumentStorage()
    private val driver = UiBoxOfficeDriver(scheduler, storage = storage, nodes = listOf(App(TestRuntime(scheduler).withEvents(marbles)) { AppInfra(it, storage) }))

    @Test
    fun `two fans go for the same seat, while a fan of another theatre takes it at theirs`(approver: Approver) {
        listOf("nyc" to 1L, "la" to 2L).forEach { (theatre, seed) ->
            Theatres(storage).create(TenantId.of(theatre), TestRuntime(scheduler).clock, Random(seed), SHOW to Layout(rows = 8, seatsPerRow = 12))
        }
        val ann = driver.fan("ann", "nyc")
        val bob = driver.fan("bob", "nyc")
        val charles = driver.fan("charles", "la")
        listOf(ann, bob, charles).forEach { it.watches(SHOW) }
        nameTheShowAndFans("nyc", "ann", "bob")
        nameTheShowAndFans("la", "charles")

        marbles.record {
            ann.reserved(SHOW, SeatRef.of("F12"))
            bob.reserved(SHOW, SeatRef.of("F12"))
            charles.reserved(SHOW, SeatRef.of("F12"))
        }

        approver.assertApproved(HtmlMarbles().render("Two fans go for the same seat, while a fan of another theatre takes it at theirs", marbles.timeline()).content)
    }

    private fun nameTheShowAndFans(theatre: String, vararg fans: String) {
        val show = storage.read { BoxOfficeTables(it.tables(TenantId.of(theatre))).shows.page().items.single().id }
        marbles.alias(show, SHOW.value)
        fans.forEach {
            marbles.alias(Shopper(show, viewerNamed(it)), it)
            marbles.alias(Actor.Fan(viewerNamed(it)), it)
        }
    }
}
