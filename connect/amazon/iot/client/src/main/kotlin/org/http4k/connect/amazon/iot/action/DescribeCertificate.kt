package org.http4k.connect.amazon.iot.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.iot.IotAction
import org.http4k.connect.amazon.iot.IotMoshi
import org.http4k.connect.amazon.iot.model.CertificateId
import org.http4k.connect.amazon.iot.model.CertificateMode
import org.http4k.connect.amazon.iot.model.CertificateStatus
import org.http4k.connect.asRemoteFailure
import org.http4k.connect.model.Timestamp
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class DescribeCertificate(
    val certificateId: CertificateId,
) : IotAction<DescribedCertificate> {

    override fun toRequest() = Request(GET, Uri.of("").path("/certificates/${certificateId.value}"))

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(IotMoshi.asA<DescribedCertificate>(bodyString()))
            else -> Failure(asRemoteFailure(this))
        }
    }
}

@JsonSerializable
data class DescribedCertificate(
    val certificateDescription: CertificateDescription,
)

@JsonSerializable
data class CertificateDescription(
    val certificateId: CertificateId,
    val certificateArn: ARN,
    val status: CertificateStatus,
    val caCertificateId: CertificateId? = null,
    val certificatePem: String? = null,
    val ownedBy: String? = null,
    val previousOwnedBy: String? = null,
    val creationDate: Timestamp? = null,
    val lastModifiedDate: Timestamp? = null,
    val customerVersion: Int? = null,
    val generationId: String? = null,
    val certificateMode: CertificateMode? = null,
    val validity: CertificateValidity? = null,
)

@JsonSerializable
data class CertificateValidity(
    val notBefore: Timestamp? = null,
    val notAfter: Timestamp? = null,
)
