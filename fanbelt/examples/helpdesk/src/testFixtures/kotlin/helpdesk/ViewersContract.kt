package helpdesk

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import helpdesk.domain.CustomerEmail
import helpdesk.domain.Subject
import org.http4k.fanbelt.testing.eventually
import org.http4k.fanbelt.testing.untilAClosedTabIsNoticed
import org.junit.jupiter.api.Test

private val di = CustomerEmail.of("di@example.com")
private val addingATeammate = Subject.of("How do I add a teammate?")

interface ViewersContract {
    val driver: HelpdeskDriver

    @Test
    fun `agents watching a ticket see who else is viewing it, live`() {
        val sam = driver.agent("sam")
        val alex = driver.agent("alex")
        val ticket = driver.customerOpens(addingATeammate, from = di)

        sam.watches(ticket)
        alex.watches(ticket)

        eventually { assertThat(sam.sees(ticket).viewers, equalTo(setOf(sam.name, alex.name))) }

        alex.leaves(ticket)

        eventually(untilAClosedTabIsNoticed) { assertThat(sam.sees(ticket).viewers, equalTo(setOf(sam.name))) }
    }
}
