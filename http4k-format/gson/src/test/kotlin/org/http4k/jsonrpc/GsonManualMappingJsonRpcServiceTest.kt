package org.http4k.jsonrpc

import com.google.gson.JsonElement
import org.http4k.format.Gson

class GsonManualMappingJsonRpcServiceTest : ManualMappingJsonRpcServiceContract<JsonElement>(Gson)
