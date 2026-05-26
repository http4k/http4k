package org.http4k.connect.amazon.iamidentitycenter.model

enum class GrantType(val wireValue: String) {
    AuthorizationCode("authorization_code"),
    AuthorizationCode2("AuthorizationCode"),
    DeviceCode("urn:ietf:params:oauth:grant-type:device_code"),
    RefreshToken("refresh_token"),
    RefreshToken2("RefreshToken");

    companion object {
        fun fromWire(wireValue: String) = entries.first { it.wireValue == wireValue }
    }
}
