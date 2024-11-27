package org.http4k.connect.github

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class GitHubToken private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<GitHubToken>(::GitHubToken)
}
