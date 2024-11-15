package org.http4k.connect.amazon.dynamodb.grammar

import org.http4k.connect.amazon.dynamodb.model.AttributeName
import org.http4k.connect.amazon.dynamodb.model.AttributeValue
import parser4k.Parser
import parser4k.commonparsers.Tokens.identifier
import parser4k.map

object ProjectionAttributeValue : ExprFactory {
    override operator fun invoke(parser: () -> Parser<Expr>): Parser<Expr> = identifier.map(::ProjectionAttributeValue)
}

fun ProjectionAttributeValue(value: String) = object : Expr {
    override fun toString(): String {
        return value
    }

    override fun eval(item: ItemWithSubstitutions): Any {
        return listOf(
            AttributeNameValue(
                AttributeName.of(value),
                (item.item[AttributeName.of(value)] ?: AttributeValue.Null())
            )
        )
    }
}
