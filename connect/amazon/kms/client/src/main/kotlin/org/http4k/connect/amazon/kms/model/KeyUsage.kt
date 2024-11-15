package org.http4k.connect.amazon.kms.model


import se.ansman.kotshi.JsonSerializable

@JsonSerializable
enum class KeyUsage {
    SIGN_VERIFY,
    ENCRYPT_DECRYPT
}
