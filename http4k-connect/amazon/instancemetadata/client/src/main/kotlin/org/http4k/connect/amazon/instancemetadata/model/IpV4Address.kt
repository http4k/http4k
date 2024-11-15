package org.http4k.connect.amazon.instancemetadata.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory

// https://stackoverflow.com/a/36760050/1253613
private val ipv4Regex = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}\$".toRegex()

class IpV4Address private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<IpV4Address>(::IpV4Address, { ipv4Regex.matches(it) })
}
