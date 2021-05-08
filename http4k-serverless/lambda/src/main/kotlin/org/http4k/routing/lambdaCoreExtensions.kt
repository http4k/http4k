package org.http4k.routing

import com.amazonaws.services.lambda.runtime.Context
import org.http4k.serverless.FunctionLoader
import org.http4k.serverless.StreamHandler
import org.http4k.serverless.lambda.AwsEnvironment.AWS_LAMBDA_FUNCTION_NAME

/**
 * Provides composite routing for functions based on the environmental name.
 */
fun functions(vararg functions: NamedFunctionLoader<Context>) = object : FunctionLoader<Context> {
    override fun invoke(env: Map<String, String>): StreamHandler<Context> {
        val name = env[AWS_LAMBDA_FUNCTION_NAME] ?: error("'AWS_LAMBDA_FUNCTION_NAME' is not set in environment")
        return functions.associateBy { it.name }[name]?.invoke(env) ?: error("Unknown function '${name}'")
    }
}
