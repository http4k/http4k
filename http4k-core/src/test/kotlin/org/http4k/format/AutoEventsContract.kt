package org.http4k.format

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.events.AutoJsonEvents
import org.http4k.events.TestEvent
import org.http4k.events.plus
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.testing.Approver
import org.http4k.testing.FileSystemApprovalSource
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.TestNamer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File
import java.io.StringWriter

open class AutoEventsContract(private val j: AutoMarshallingJson) {

    @RegisterExtension
    @JvmField
    val appTest = JsonApprovalTest(
        TestNamer.MethodOnly,
        FileSystemApprovalSource(File("../http4k-core/src/test/resources")))

    @Test
    fun `event serialises to json`(approver: Approver) {
        val final = TestEvent() + ("first" to "1") + ("second" to 2)
        val w = StringWriter()

        AutoJsonEvents(j, w::write)(final)

        approver.assertApproved(Response(OK).with(CONTENT_TYPE of APPLICATION_JSON).body(w.toString()))
    }
}