package org.http4k.connect.amazon.kms.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
enum class EncryptionAlgorithm {
    SYMMETRIC_DEFAULT,
    RSAES_OAEP_SHA_1,
    RSAES_OAEP_SHA_256
}
