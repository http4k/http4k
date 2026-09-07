package org.http4k.connect.amazon.iot.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

/** The SHA-256 of the DER-encoded certificate, as 64 hex characters. */
class CertificateId private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<CertificateId>(::CertificateId, "[a-fA-F0-9]{64}".regex)
}
