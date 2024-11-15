package org.http4k.connect.gitlab

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class GitLabToken private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<GitLabToken>(::GitLabToken)
}
