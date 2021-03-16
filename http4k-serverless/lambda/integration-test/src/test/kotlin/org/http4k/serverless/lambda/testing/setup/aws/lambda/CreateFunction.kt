package org.http4k.serverless.lambda.testing.setup.aws.lambda

import com.fasterxml.jackson.annotation.JsonProperty
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.serverless.lambda.testing.setup.aws.kClass
import org.http4k.serverless.lambda.testing.setup.aws.lambda.LambdaJackson.auto
import java.util.*

class CreateFunction(private val functionPackage: FunctionPackage) : LambdaAction<FunctionDetailsData>(kClass()) {
    override fun toRequest(): Request {
        val code = String(Base64.getEncoder().encode(functionPackage.jar.array()))
        return Request(Method.POST, Uri.of("/2015-03-31/functions/"))
            .with(
                Body.auto<CreateFunction>().toLens() of CreateFunction(
                    Code(code),
                    functionPackage.name.value,
                    functionPackage.handler.value,
                    functionPackage.role.name,
                    Environment(functionPackage.environmentProperties),
                    functionPackage.timeoutInSeconds)
            )
    }

    private data class CreateFunction(@JsonProperty("Code") val code: Code,
                                      @JsonProperty("FunctionName") val functionName: String,
                                      @JsonProperty("Handler") val handler: String,
                                      @JsonProperty("Role") val role: String,
                                      @JsonProperty("Environment") val environment: Environment,
                                      @JsonProperty("Timeout") val timeout: Int = 3,
                                      @JsonProperty("Runtime") val runtime: String = "java8",
                                      @JsonProperty("MemorySize") val memory: Int = 128)

    private data class Code(@JsonProperty("ZipFile") val zipFile: String)
    private data class Environment(@JsonProperty("Variables") val variables: Map<String, String>)


}
