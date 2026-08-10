package org.http4k.connect.amazon.iot.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.iot.IotAction
import org.http4k.connect.amazon.iot.IotMoshi
import org.http4k.connect.amazon.iot.model.JobId
import org.http4k.connect.amazon.iot.model.JobStatus
import org.http4k.connect.amazon.iot.model.TargetSelection
import org.http4k.connect.amazon.iot.model.TimeoutConfig
import org.http4k.connect.asRemoteFailure
import org.http4k.connect.model.Timestamp
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class DescribeJob(
    val jobId: JobId,
) : IotAction<DescribedJob> {

    override fun toRequest() = Request(GET, Uri.of("").path("/jobs/${jobId.value}"))

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(IotMoshi.asA<DescribedJob>(bodyString()))
            else -> Failure(asRemoteFailure(this))
        }
    }
}

@JsonSerializable
data class DescribedJob(
    val job: Job,
    val documentSource: String? = null,
)

/**
 * The subset of the AWS Job object which this module supports. Note that the job document is
 * not part of this shape: devices read it through the jobs data plane, and the control plane
 * only exposes it via GetJobDocument (unsupported here).
 */
@JsonSerializable
data class Job(
    val jobArn: ARN,
    val jobId: JobId,
    val targets: List<ARN> = emptyList(),
    val targetSelection: TargetSelection? = null,
    val status: JobStatus? = null,
    val forceCanceled: Boolean? = null,
    val comment: String? = null,
    val createdAt: Timestamp? = null,
    val lastUpdatedAt: Timestamp? = null,
    val completedAt: Timestamp? = null,
    val description: String? = null,
    val timeoutConfig: TimeoutConfig? = null,
)
