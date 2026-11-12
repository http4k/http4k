package helpdesk

import helpdesk.domain.CustomerEmail
import helpdesk.domain.Subject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CustomerCardTest {
    private val driver = UiHelpdeskDriver()

    @Test
    fun `the customer card is a one-shot fragment that closes back to an empty placeholder`() {
        val sam = driver.agent("sam")
        val billing = driver.customerOpens(Subject.of("Billing question"), from = CustomerEmail.of("bo@example.com"))

        sam.previewsCustomerOf(billing)
        assertEquals(true, sam.seesPreview())

        sam.closesPreview()
        assertEquals(false, sam.seesPreview())
    }
}