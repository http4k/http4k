package org.http4k.connect.langchain.embedding

import dev.langchain4j.model.embedding.EmbeddingModel
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
interface EmbeddingModelContract {
    val model: EmbeddingModel

    @Test
    fun `can call through to language model`(approver: Approver) {
        approver.assertApproved(model.embed("hello kitty").content().vectorAsList().toString())
    }
}
