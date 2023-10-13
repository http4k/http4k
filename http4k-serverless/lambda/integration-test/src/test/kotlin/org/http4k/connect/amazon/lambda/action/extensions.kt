package org.http4k.connect.amazon.lambda.action

import dev.forkhandles.result4k.map
import org.http4k.connect.amazon.getOrThrow
import org.http4k.connect.amazon.lambda.Lambda
import org.http4k.connect.amazon.lambda.model.Function
import org.http4k.connect.amazon.lambda.model.FunctionDetails
import org.http4k.connect.amazon.lambda.model.FunctionPackage

fun Lambda.createFunction(functionPackage: FunctionPackage): FunctionDetails =
    this(CreateFunction(functionPackage)).map { FunctionDetails(it.arn, it.name) }.getOrThrow()

fun Lambda.setPermission(details: FunctionDetails, permission: Permission) =
    this(SetFunctionPermission(details.arn, permission))

fun Lambda.delete(function: Function) = this(DeleteFunction(function)).getOrThrow()
fun Lambda.list() = this(ListFunctions()).getOrThrow()
