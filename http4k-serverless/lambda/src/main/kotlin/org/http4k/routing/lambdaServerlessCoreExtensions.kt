package org.http4k.routing

import com.amazonaws.services.lambda.runtime.Context
import org.http4k.serverless.FunctionLoader
import org.http4k.serverless.StreamHandler
import org.http4k.serverless.lambda.AwsEnvironment


/**
 * Provides composite routing for functions based on the environmental name.
 */
fun functions(vararg functions: NamedFunctionLoader<Context>) =
    FunctionLoader<Context> { env ->
        val name = env[AwsEnvironment.AWS_LAMBDA_FUNCTION_NAME] ?: error("'${AwsEnvironment.AWS_LAMBDA_FUNCTION_NAME}' is not set in environment")
        functions.associateBy { it.name }[name]?.invoke(env) ?: error("Unknown function '${name}'")
    }

infix fun String.bind(sh: StreamHandler<Context>): NamedFunctionLoader<Context> = this bind { _: Map<String, String> ->
    sh
}

infix fun String.bind(fn: FunctionLoader<Context>) = NamedFunctionLoader(this, fn)
