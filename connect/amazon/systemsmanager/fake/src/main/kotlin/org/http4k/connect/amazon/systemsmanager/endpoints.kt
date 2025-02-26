package org.http4k.connect.amazon.systemsmanager

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.systemsmanager.action.DeleteParameter
import org.http4k.connect.amazon.systemsmanager.action.GetParameter
import org.http4k.connect.amazon.systemsmanager.action.GetParameters
import org.http4k.connect.amazon.systemsmanager.action.ParameterValue
import org.http4k.connect.amazon.systemsmanager.action.GetParametersResult
import org.http4k.connect.amazon.systemsmanager.action.PutParameter
import org.http4k.connect.amazon.systemsmanager.action.PutParameterResult
import org.http4k.connect.amazon.systemsmanager.model.Parameter
import org.http4k.connect.model.Timestamp
import org.http4k.connect.storage.Storage


fun AwsJsonFake.deleteParameter(parameters: Storage<StoredParameter>) = route<DeleteParameter> { req ->
    parameters[req.Name.value]?.let {
        parameters.remove(req.Name.value)
        Unit
    }
}


fun AwsJsonFake.getParameter(parameters: Storage<StoredParameter>) = route<GetParameter> { req ->
    parameters[req.Name.value]?.let { ParameterValue(it.parameter()) }
}

fun AwsJsonFake.getParameters(parameters: Storage<StoredParameter>) = route<GetParameters> { req ->
    GetParametersResult(
        req.Names.filter { parameters[it.value] == null }.map { it.value },
        req.Names.mapNotNull { parameters[it.value]?.parameter() }
    )
}

private fun StoredParameter.parameter() = Parameter(
    ARN.of(
        SystemsManager.awsService,
        Region.of("us-east-1"),
        AwsAccount.of("0"),
        "parameter",
        name
    ),
    name, value, type, null, 1, Timestamp.of(0), null, null
)


fun AwsJsonFake.putParameter(parameters: Storage<StoredParameter>) = route<PutParameter> { req ->
    val current = parameters[req.Name.value]
    when {
        current == null -> {
            parameters[req.Name.value] = StoredParameter(req.Name, req.Value, req.Type, 1)
            PutParameterResult("Standard", 1)
        }

        req.Overwrite == true -> {
            parameters[req.Name.value] = StoredParameter(req.Name, req.Value, req.Type, current.version + 1)
            PutParameterResult("Standard", current.version + 1)
        }

        else -> null
    }
}
