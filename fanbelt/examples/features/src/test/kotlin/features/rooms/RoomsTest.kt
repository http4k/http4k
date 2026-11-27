package features.rooms

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import features.notes.NotesFeature
import features.notes.Shown
import features.viewerNamed
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
        assertThat(tenant { execute(rooms.create, ann, NewRoom("plans", private = false)) }, equalTo(Verdict.Accepted))

        assertThat(tenant { read(rooms.rooms).map { it.id } }, equalTo(listOf(plans)))
    }

    @Test
    fun `a name that's taken is refused`() {
        tenant { given(rooms.create, ann, NewRoom("plans", private = false)) }

        assertThat(tenant { execute(rooms.create, bob, NewRoom("plans", private = true)) }, equalTo(Verdict.Rejected("That name is taken")))
    }

    @Test
    fun `a private room is hidden from everyone but its owner`() {
        tenant { given(rooms.create, ann, NewRoom("plans", private = true)) }

        assertThat(tenant { read(rooms.visibleRoom, bob, plans) }, equalTo(Hidden))
        assertThat(tenant { execute(rooms.say, bob, Said(plans, "hello")) }, equalTo(Verdict.Hidden))
    }

    @Test
    fun `only its owner may rename a room`() {
        tenant { given(rooms.create, ann, NewRoom("plans", private = false)) }

        assertThat(tenant { execute(rooms.rename, bob, Rename(plans, "Bob's now")) }, equalTo(Verdict.Denied))
        assertThat(tenant { execute(rooms.rename, ann, Rename(plans, "Summer plans")) }, equalTo(Verdict.Accepted))
        assertThat(tenant { read(rooms.room, plans)?.title?.value }, equalTo("Summer plans"))
    }

    @Test
    fun `what was said since a viewer was last in the room is new to them`() {
        tenant { given(rooms.create, ann, NewRoom("plans", private = false)) }
        tenant { given(rooms.say, ann, Said(plans, "one")) }
        tenant { open(rooms.enter, bob, plans) }.close()

        tenant { given(rooms.say, ann, Said(plans, "two")) }
        tenant { given(rooms.say, ann, Said(plans, "three")) }

        assertThat(tenant { read(rooms.unseen, Reader(plans, bob)) }, equalTo(2))
    }

    @Test
    fun `a viewer in the room is here, until they leave`() {
        tenant { given(rooms.create, ann, NewRoom("plans", private = false)) }
        val visit = tenant { open(rooms.enter, bob, plans) }

        assertThat(tenant { read(rooms.here, plans).keys }, equalTo(setOf(bob)))

        visit.close()
        assertThat(tenant { read(rooms.here, plans).keys }, equalTo(emptySet()))
    }

    @Test
    fun `a new room is announced on the notes board`() {
        tenant { given(rooms.create, ann, NewRoom("plans", private = false)) }

        assertThat(tenant { read(notes.newest, Shown.of(20)).notes.map { it.text.value } }, equalTo(listOf("Room plans opened")))
    }
}
