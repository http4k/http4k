package org.http4k.connect.kafka.rest.extensions

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.kafka.rest.model.PartitionId
import org.junit.jupiter.api.Test

class StickyKeyPartitionerTest {
    @Test
    fun `uses the key hash for getting the partition`() {
        val p = StickyKeyRecordPartitioner<String, String>(
            listOf(
                PartitionId.of(1),
                PartitionId.of(2),
                PartitionId.of(3),
            )
        )

        assertThat(p("hello", "world"), equalTo(PartitionId.of(2)))
        assertThat(p("hello", "world2"), equalTo(PartitionId.of(2)))
        assertThat(p("world", "world2"), equalTo(PartitionId.of(1)))
    }
}
