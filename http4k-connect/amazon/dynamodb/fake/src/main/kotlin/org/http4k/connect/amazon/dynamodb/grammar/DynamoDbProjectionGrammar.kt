package org.http4k.connect.amazon.dynamodb.grammar

import parser4k.OutputCache
import parser4k.Parser
import parser4k.oneOf
import parser4k.oneOfWithPrecedence
import parser4k.parseWith
import parser4k.reset
import parser4k.with

object DynamoDbProjectionGrammar {
    private val cache = OutputCache<Expr>()

    fun parse(expression: String): Expr = expression.parseWith(expr)

    private val expr: Parser<Expr> =
        oneOf(
            ProjectionList(::expr).with(cache),
            oneOfWithPrecedence(
                ProjectionMapAttributeValue(::expr).with(cache),
                ProjectionIndexedAttributeValue(::expr).with(cache),
            ),
            oneOf(
                ExpressionAttributeName.projection(::expr).with(cache),
                ExpressionAttributeValue(::expr).with(cache),
                ProjectionAttributeValue(::expr).with(cache)
            )
        )
            .reset(cache)
}
