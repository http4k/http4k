package helpdesk

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import helpdesk.domain.CustomerEmail
import helpdesk.domain.Subject
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
}