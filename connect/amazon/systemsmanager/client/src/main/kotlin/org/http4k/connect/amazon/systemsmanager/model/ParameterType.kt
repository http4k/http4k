package org.http4k.connect.amazon.systemsmanager.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
enum class ParameterType {
    String, StringList, SecureString
}
