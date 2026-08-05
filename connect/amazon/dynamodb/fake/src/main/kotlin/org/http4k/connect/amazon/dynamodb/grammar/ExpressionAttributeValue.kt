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

/**
 * A `:token` reference to the request's `ExpressionAttributeValues`. DynamoDB rejects an undefined token as
 * a bad *request* rather than failing on the data, so it is raised as a [DynamoDbConditionError] - and it is
 * resolved in [Expr.validate] as well as in `eval`, so an `AND`/`OR` which never evaluates this branch still
 * reports it.
 */
fun ExpressionAttributeValue(value: String): Expr = object : Expr {
    override fun eval(item: ItemWithSubstitutions) = item.resolveValue(value)

    override fun validate(item: ItemWithSubstitutions) {
        item.resolveValue(value)
    }
}

private fun ItemWithSubstitutions.resolveValue(value: String) = values[":$value"]
    ?: throw DynamoDbConditionError(
        "An expression attribute value used in expression is not defined; attribute value: :$value"
    )
