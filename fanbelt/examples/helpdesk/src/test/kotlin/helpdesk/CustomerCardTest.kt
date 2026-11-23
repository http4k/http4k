package helpdesk

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import helpdesk.domain.CustomerEmail
import helpdesk.domain.Subject
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.junit.jupiter.api.Test

class CustomerCardTest {
    private val driver = UiHelpdeskDriver()

    @Test
    fun `the customer card is a one-shot fragment that closes back to an empty placeholder`() {
        val sam = driver.agent("sam")
        val billing = driver.customerOpens(Subject.of("Billing question"), from = CustomerEmail.of("bo@example.com"))

        sam.previewsCustomerOf(billing)
        assertThat(sam.seesPreview(), equalTo(true))

        sam.closesPreview()
        assertThat(sam.seesPreview(), equalTo(false))
    }

    @Test
    fun `an agent the org removes can neither preview a customer nor be sent to the queue, while others can`() {
        val sam = driver.agent("sam")
        val alex = driver.agent("alex")
        val billing = driver.customerOpens(Subject.of("Billing question"), from = CustomerEmail.of("bo@example.com"))
        driver.orgRemoves("sam")

        assertThat(sam.gets("/t/$billing/customer"), equalTo(FORBIDDEN))
        assertThat(sam.gets("/"), equalTo(FORBIDDEN))
        assertThat(alex.gets("/t/$billing/customer"), equalTo(OK))
        assertThat(alex.gets("/"), equalTo(SEE_OTHER))
    }
}
