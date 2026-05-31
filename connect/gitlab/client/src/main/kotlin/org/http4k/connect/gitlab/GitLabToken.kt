package org.http4k.connect.gitlab

import dev.forkhandles.values.Maskers.hidden
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class GitLabToken private constructor(value: String) : StringValue(value, hidden()) {
    companion object : NonBlankStringValueFactory<GitLabToken>(::GitLabToken)
}
