package org.http4k.jsonrpc

import com.beust.klaxon.JsonObject
import org.http4k.format.Klaxon

class KlaxonManualMappingJsonRpcServiceTest : ManualMappingJsonRpcServiceContract<JsonObject>(Klaxon)
