package org.http4k.connect.amazon.ses

import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.http4k.connect.amazon.ses.model.RawMessageBase64
import org.http4k.connect.amazon.ses.model.of
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(ApprovalTest::class)
class JakartaMailExtensionsTest {

    @Test
    fun `serialize mime message`(approver: Approver) {
        val message = object: MimeMessage(null as Session?){
            override fun updateMessageID() {
                setHeader("Message-ID", "<message1337>")
            }

            override fun setSentDate(d: Date?) {}
        }.apply {
            setFrom("foo@bar.com")
            addRecipients(Message.RecipientType.TO, "toll@troll.com")
            subject = "hello"
            setText("world")
        }

        val raw = RawMessageBase64.of(message)
        approver.assertApproved(raw.decode())
    }
}

