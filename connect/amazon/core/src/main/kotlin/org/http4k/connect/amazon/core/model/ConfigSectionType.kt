package org.http4k.connect.amazon.core.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class ConfigSectionType private constructor(value: String) : StringValue(value) {

    companion object : NonBlankStringValueFactory<ConfigSectionType>(::ConfigSectionType) {
        val profile = ConfigSectionType("profile")
        val ssoSession = ConfigSectionType("sso-session")
    }
}
