package org.http4k.datastar

import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.DatastarEvent.MergeFragments
import org.http4k.lens.datastarFragments
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(DatastarFragmentApprovalTest::class)
class DatastarFragmentApprovalTestTest {

    @Test
    fun `approves datastar fragments`(approver: Approver) {
        approver.assertApproved(Response(OK).datastarFragments(MergeFragments("<div/>", "<span/>")))
    }
}
