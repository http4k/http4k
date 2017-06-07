package org.http4k.format

import com.google.gson.JsonElement

class GsonAutoTest : AutoMarshallingContract<JsonElement>(Gson)
class GsonTest : JsonContract<JsonElement, JsonElement>(Gson)
class GsonJsonErrorResponseRendererContractTest : JsonErrorResponseRendererContract<JsonElement, JsonElement>(Gson)
class GsonGenerateDataClassesTest : GenerateDataClassesContract<JsonElement, JsonElement>(Gson)