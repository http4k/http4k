package org.http4k.connect.langchain.chat

import dev.langchain4j.model.image.ImageModel
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
interface ImageModelContract {
    val model: ImageModel

    @Test
    fun `can call through to language model`(approver: Approver) {
        approver.assertApproved(model.generate("hello kitty").content().base64Data())
    }
}
