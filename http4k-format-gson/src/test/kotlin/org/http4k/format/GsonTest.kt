package org.http4k.format

import com.google.gson.JsonElement

class GsonTest : JsonContract<JsonElement, JsonElement>(Gson)

class GsonJsonErrorResponseRendererContractTest : JsonErrorResponseRendererContract<JsonElement, JsonElement>(Gson)