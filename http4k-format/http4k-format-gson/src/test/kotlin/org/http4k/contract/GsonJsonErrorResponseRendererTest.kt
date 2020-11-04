package org.http4k.contract

import com.google.gson.JsonElement
import org.http4k.format.Gson

class GsonJsonErrorResponseRendererTest : JsonErrorResponseRendererContract<JsonElement>(Gson)