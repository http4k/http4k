package org.http4k.lens

import org.http4k.connect.github.webhook.WebhookEventType
import org.http4k.core.ContentType
import org.http4k.core.HttpMessage

val Header.X_GITHUB_DELIVERY get() = Header.uuid().required("X-GitHub-Delivery")
val Header.X_GITHUB_EVENT get() = Header.enum<HttpMessage, WebhookEventType>().required("X-GitHub-Event")
val Header.X_HUB_SIGNATURE_256
    get() = Header
        .map({ it.split("sha256=")[1] }, { "sha256=$it" })
        .defaulted("X-Hub-Signature-256", "")

val ContentType.Companion.GITHUB_JSON get() = ContentType("application/vnd.github+json")
