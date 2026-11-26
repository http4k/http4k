package features.rooms

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import features.notes.NotesFeature
import features.notes.Shown
import features.viewerNamed
import org.http4k.fanbelt.feature.ActorKey
import org.http4k.fanbelt.feature.Outcome.Hidden
import org.http4k.fanbelt.testing.TestTenant
import org.http4k.fanbelt.testing.Verdict
import org.junit.jupiter.api.Test

class RoomsTest {
    private val tenant = TestTenant()
    private val notes = NotesFeature(tenant.runtime.random)
    private val announcements = AnnouncementsFeature(notes, tenant.runtime.random)
    private val rooms = RoomsFeature(announcements, tenant.runtime.random)
    private val ann = viewerNamed("ann")
    private val bob = viewerNamed("bob")
    private val plans = RoomName.of("plans")

    init {
        tenant.runs(announcements.opened)
    }

    @Test
    fun `a new room is listed for everyone`() {
        assertThat(tenant { rooms.create.execute(ann, NewRoom("plans", private = false)) }, equalTo(Verdict.Accepted))

        assertThat(tenant { rooms.rooms.read().map { it.id } }, equalTo(listOf(plans)))
    }

    @Test
    fun `a name that's taken is refused`() {
        tenant { rooms.create.given(ann, NewRoom("plans", private = false)) }

        assertThat(tenant { rooms.create.execute(bob, NewRoom("plans", private = true)) }, equalTo(Verdict.Rejected("That name is taken")))
    }

    @Test
    fun `a private room is hidden from everyone but its owner`() {
        tenant { rooms.create.given(ann, NewRoom("plans", private = true)) }

        assertThat(tenant { rooms.visibleRoom.read(ActorKey(bob, plans)) }, equalTo(Hidden))
        assertThat(tenant { rooms.say.execute(bob, Said(plans, "hello")) }, equalTo(Verdict.Hidden))
    }

    @Test
    fun `only its owner may rename a room`() {
        tenant { rooms.create.given(ann, NewRoom("plans", private = false)) }

        assertThat(tenant { rooms.rename.execute(bob, Rename(plans, "Bob's now")) }, equalTo(Verdict.Denied))
        assertThat(tenant { rooms.rename.execute(ann, Rename(plans, "Summer plans")) }, equalTo(Verdict.Accepted))
        assertThat(tenant { rooms.room.read(plans)?.title?.value }, equalTo("Summer plans"))
    }

    @Test
    fun `what was said since a viewer was last in the room is new to them`() {
        tenant { rooms.create.given(ann, NewRoom("plans", private = false)) }
        tenant { rooms.say.given(ann, Said(plans, "one")) }
        tenant { rooms.enter.open(bob, plans) }.close()

        tenant { rooms.say.given(ann, Said(plans, "two")) }
        tenant { rooms.say.given(ann, Said(plans, "three")) }

        assertThat(tenant { rooms.unseen.read(Reader(plans, bob)) }, equalTo(2))
    }

    @Test
    fun `a viewer in the room is here, until they leave`() {
        tenant { rooms.create.given(ann, NewRoom("plans", private = false)) }
        val visit = tenant { rooms.enter.open(bob, plans) }

        assertThat(tenant { rooms.here.read(plans).keys }, equalTo(setOf(bob)))

        visit.close()
        assertThat(tenant { rooms.here.read(plans).keys }, equalTo(emptySet()))
    }

    @Test
    fun `a new room is announced on the notes board`() {
        tenant { rooms.create.given(ann, NewRoom("plans", private = false)) }

        assertThat(tenant { notes.newest.read(Shown.of(20)).notes.map { it.text.value } }, equalTo(listOf("Room plans opened")))
    }
}
