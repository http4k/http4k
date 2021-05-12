package org.http4k.routing

import com.amazonaws.services.lambda.runtime.Context
import org.http4k.serverless.FnHandler
import org.http4k.serverless.FnLoader
import org.http4k.serverless.lambda.AwsEnvironment.AWS_LAMBDA_FUNCTION_NAME
import java.io.InputStream

/**
 * Provides composite routing for functions based on the environmental name.
 */
fun functions(vararg functions: NamedFnLoader<Context>) =
    object : FnLoader<Context> {
        override fun invoke(env: Map<String, String>): FnHandler<InputStream, Context, InputStream> {
            val name = env[AWS_LAMBDA_FUNCTION_NAME] ?: error("'$AWS_LAMBDA_FUNCTION_NAME' is not set in environment")
            return functions.associateBy { it.name }[name]?.invoke(env) ?: error("Unknown function '${name}'")
        }
    }
