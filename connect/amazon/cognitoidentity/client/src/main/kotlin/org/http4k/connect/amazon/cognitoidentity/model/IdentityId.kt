package org.http4k.connect.amazon.cognitoidentity.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

class IdentityId private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<IdentityId>(::IdentityId, "[\\w-]+:[0-9a-f-]+".regex)
}
