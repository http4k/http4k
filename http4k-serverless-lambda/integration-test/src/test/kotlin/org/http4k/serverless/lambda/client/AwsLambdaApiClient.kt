package org.http4k.serverless.lambda.client

import com.fasterxml.jackson.annotation.JsonProperty
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import java.nio.ByteBuffer
import java.util.Base64

class AwsLambdaApiClient(client: HttpHandler, region: Region) {
    private val client = LambdaApi(region).then(client)

    fun create(functionPackage: FunctionPackage) {
        val code = String(Base64.getEncoder().encode(functionPackage.jar.array()))
        val request = Request(POST, Uri.of("/2015-03-31/functions/"))
            .with(createFunctionBody of CreateFunction(Code(code),
                functionPackage.name.value,
                functionPackage.handler.value,
                functionPackage.role.name,
                Environment(functionPackage.environmentProperties),
                functionPackage.timeoutInSeconds))
        val response = client(request)
        if (!response.status.successful) {
            throw RuntimeException("Could not create function (error ${response.status.code}): ${response.bodyString()}")
        }
    }

    fun delete(functionName: FunctionName) {
        client(Request(DELETE, Uri.of("/2015-03-31/functions/${functionName.value}")))
    }

    fun list() =
        listFunctionBody.extract(client(Request(GET, Uri.of("/2015-03-31/functions/"))))

    companion object {
        private val createFunctionBody = Body.auto<CreateFunction>().toLens()
        private val listFunctionBody = Body.auto<ListFunctionsResponse>()
            .map { response -> response.functions.map { FunctionDetails(it.arn, it.name) } }
            .toLens()
    }

    private data class Code(@JsonProperty("ZipFile") val zipFile: String)

    private data class CreateFunction(@JsonProperty("Code") val code: Code,
                                      @JsonProperty("FunctionName") val functionName: String,
                                      @JsonProperty("Handler") val handler: String,
                                      @JsonProperty("Role") val role: String,
                                      @JsonProperty("Environment") val environment: Environment,
                                      @JsonProperty("Timeout") val timeout: Int = 3,
                                      @JsonProperty("Runtime") val runtime: String = "java8")

    private data class Environment(@JsonProperty("Variables") val variables: Map<String, String>)

    private data class FunctionDetailsData(
        @JsonProperty("FunctionArn") val arn: String,
        @JsonProperty("FunctionName") val name: String
    )

    private data class ListFunctionsResponse(@JsonProperty("Functions") val functions: List<FunctionDetailsData>)
}

data class FunctionPackage(
    val name: FunctionName,
    val handler: FunctionHandler,
    val jar: ByteBuffer,
    val role: Role,
    val environmentProperties: Map<String, String> = mapOf(),
    val timeoutInSeconds: Int = 15
)

data class FunctionName(val value: String)

data class FunctionHandler(val value: String){
    companion object{
        val http4kFunctionHandler = FunctionHandler("org.http4k.serverless.lambda.LambdaFunction::handle")
    }
}

data class FunctionDetails(val arn: String, val name: String)

data class Role(val name: String)

data class Region(val name: String)
