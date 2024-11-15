package org.http4k.connect.amazon.dynamodb.grammar

import parser4k.Parser
import parser4k.commonparsers.Tokens
import parser4k.inOrder
import parser4k.map
import parser4k.oneOf
import parser4k.skipFirst

object ExpressionAttributeValue : ExprFactory {
    override operator fun invoke(parser: () -> Parser<Expr>): Parser<Expr> = inOrder(oneOf(':'), Tokens.identifier)
        .skipFirst().map(::ExpressionAttributeValue)
}

fun ExpressionAttributeValue(value: String) = Expr { item ->
    item.values[":$value"] ?: error("missing value $value")
}
