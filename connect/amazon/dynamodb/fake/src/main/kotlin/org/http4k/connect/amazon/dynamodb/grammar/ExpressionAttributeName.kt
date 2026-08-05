package org.http4k.connect.amazon.dynamodb.grammar

import org.http4k.connect.amazon.dynamodb.model.AttributeValue
import parser4k.Parser
import parser4k.commonparsers.Tokens
import parser4k.inOrder
import parser4k.map
import parser4k.oneOf
import parser4k.parseWith
import parser4k.skipFirst

/**
 * A `#token` reference to the request's `ExpressionAttributeNames`. As for [ExpressionAttributeValue], a
 * token which is not defined there is a request-level [DynamoDbConditionError] rather than a server error,
 * and is resolved in [Expr.validate] as well as in `eval`.
 */
object ExpressionAttributeName : ExprFactory {
    override operator fun invoke(parser: () -> Parser<Expr>): Parser<Expr> = inOrder(oneOf('#'), Tokens.identifier)
        .skipFirst().map { value ->
            object : Expr {
                override fun eval(item: ItemWithSubstitutions) =
                    item.item[item.resolveName(value)] ?: AttributeValue.Null()

                override fun validate(item: ItemWithSubstitutions) {
                    item.resolveName(value)
                }
            }
        }

    fun projection(parser: () -> Parser<Expr>): Parser<Expr> = inOrder(oneOf('#'), Tokens.identifier)
        .skipFirst().map { value ->
            Expr { item -> item.resolveName(value).value.parseWith(parser()).eval(item) }
        }
}

private fun ItemWithSubstitutions.resolveName(name: String) = names["#$name"]
    ?: throw DynamoDbConditionError(
        "An expression attribute name used in the document path is not defined; attribute name: #$name"
    )
