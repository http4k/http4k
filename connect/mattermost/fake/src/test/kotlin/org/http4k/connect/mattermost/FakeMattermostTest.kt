package org.http4k.connect.mattermost

import org.http4k.core.Uri

class FakeMattermostTest : MattermostContract() {
    override val http = FakeMattermost()
    override val uri: Uri = Uri.of("http://localhost")
}
