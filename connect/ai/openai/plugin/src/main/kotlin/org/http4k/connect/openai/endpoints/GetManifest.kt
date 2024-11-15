package org.http4k.connect.openai.endpoints

import org.http4k.connect.openai.model.Manifest
import org.http4k.connect.openai.model.ManifestJson.auto
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.routing.bind

internal fun GetManifest(manifest: Manifest) = "/.well-known/ai-plugin.json" bind GET to {
    Response(OK).with(Body.auto<Manifest>().toLens() of manifest)
}
