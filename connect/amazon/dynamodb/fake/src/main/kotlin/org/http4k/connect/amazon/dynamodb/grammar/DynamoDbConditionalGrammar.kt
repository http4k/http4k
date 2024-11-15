package org.http4k.connect.amazon.dynamodb.grammar

import parser4k.OutputCache
import parser4k.Parser
import parser4k.nestedPrecedence
import parser4k.oneOf
import parser4k.oneOfWithPrecedence
import parser4k.parseWith
import parser4k.reset
import parser4k.with

object DynamoDbConditionalGrammar {
    private val cache = OutputCache<Expr>()

    fun parse(expression: String): Expr = expression.parseWith(expr)

    private val expr: Parser<Expr> = oneOfWithPrecedence(
        And(::expr).with(cache),
        Not(::expr).with(cache),
        Or(::expr).with(cache),
        Between(::expr).with(cache),
        oneOf(
            Equal(::expr).with(cache),
            NotEqual(::expr).with(cache),
            LessThan(::expr).with(cache),
            LessThanOrEqual(::expr).with(cache),
            GreaterThan(::expr).with(cache),
            GreaterThanOrEqual(::expr).with(cache)
        ),
        oneOf(
            In(::expr).with(cache),
            Size(::expr).with(cache)
        ),
        oneOf(
            AttributeExists(::expr).with(cache),
            AttributeNotExists(::expr).with(cache),
            AttributeType(::expr).with(cache),
            BeginsWith(::expr).with(cache),
            Contains(::expr).with(cache)
        ),
        Paren(::expr).with(cache).nestedPrecedence(),
        oneOfWithPrecedence(
            ExpressionAttributeName(::expr).with(cache),
            MapAttributeValue(::expr).with(cache),
            IndexedAttributeValue(::expr).with(cache),
            ExpressionAttributeValue(::expr).with(cache),
            ConditionAttributeValue(::expr).with(cache)
        )
    ).reset(cache)
}
