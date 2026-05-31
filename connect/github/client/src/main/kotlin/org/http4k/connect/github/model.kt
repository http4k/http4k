package org.http4k.connect.github

import dev.forkhandles.values.Maskers.hidden
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class GitHubToken private constructor(value: String) : StringValue(value, hidden()) {
    companion object : NonBlankStringValueFactory<GitHubToken>(::GitHubToken)
}
