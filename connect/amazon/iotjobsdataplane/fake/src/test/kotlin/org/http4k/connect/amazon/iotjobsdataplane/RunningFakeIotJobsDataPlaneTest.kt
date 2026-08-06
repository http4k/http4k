package org.http4k.connect.amazon.iotjobsdataplane

import org.http4k.connect.WithRunningFake
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.iot.FakeIot
import org.http4k.connect.amazon.iot.StoredJob
import org.http4k.connect.amazon.iot.createJob
import org.http4k.connect.amazon.iot.deleteJob
import org.http4k.connect.amazon.iotjobsdataplane.model.JobId
import org.http4k.connect.amazon.iotjobsdataplane.model.ThingName
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.connect.successValue
import org.junit.jupiter.api.BeforeEach
import org.http4k.connect.amazon.iot.model.JobId as IotJobId

/**
 * The data plane runs over the wire; jobs are seeded through an in-memory FakeIot sharing
 * the same store. The store is necessarily class-level (the fake factory runs before any
 * instance exists), so it is emptied before each test.
 */
class RunningFakeIotJobsDataPlaneTest :
    IotJobsDataPlaneContract, FakeAwsContract, WithRunningFake({ FakeIotJobsDataPlane(store) }) {

    override val thingName = ThingName.of("my-thing")

    private val thingArn = ARN.of("arn:aws:iot:ldn-north-1:000000000000:thing/${thingName.value}")

    @BeforeEach
    fun emptyTheStore() {
        store.removeAll()
    }

    override fun createJob(jobId: JobId, document: String) {
        FakeIot(store).client().createJob(IotJobId.of(jobId.value), listOf(thingArn), document).successValue()
    }

    override fun cleanupJob(jobId: JobId) {
        FakeIot(store).client().deleteJob(IotJobId.of(jobId.value), force = true)
    }

    companion object {
        private val store = Storage.InMemory<StoredJob>()
    }
}
