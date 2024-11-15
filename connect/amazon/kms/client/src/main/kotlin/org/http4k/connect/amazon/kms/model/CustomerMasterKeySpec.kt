package org.http4k.connect.amazon.kms.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
enum class CustomerMasterKeySpec {
    RSA_2048,
    RSA_3072,
    RSA_4096,
    ECC_NIST_P256,
    ECC_NIST_P384,
    ECC_NIST_P521,
    ECC_SECG_P256K1,
    SYMMETRIC_DEFAULT
}
