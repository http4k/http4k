package org.http4k.connect.amazon.iamidentitycenter.oidc.action

enum class GrantType(val wireValue: String) {
    AuthorizationCode("authorization_code"),
    DeviceCode("urn:ietf:params:oauth:grant-type:device_code"),
    RefreshToken("refresh_token");

    companion object {
        fun fromWire(wireValue: String) = entries.first { it.wireValue == wireValue }
    }
}
