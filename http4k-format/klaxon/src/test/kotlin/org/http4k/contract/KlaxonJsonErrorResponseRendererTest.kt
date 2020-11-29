package org.http4k.contract

import com.beust.klaxon.JsonObject
import org.http4k.format.Klaxon

class KlaxonJsonErrorResponseRendererTest : JsonErrorResponseRendererContract<JsonObject>(Klaxon)
