package org.http4k.connect.amazon.dynamodb.grammar

import org.http4k.connect.amazon.dynamodb.model.AttributeValue
import parser4k.Parser
import parser4k.commonparsers.Tokens.number
import parser4k.commonparsers.token
import parser4k.inOrder
import parser4k.mapLeftAssoc
import parser4k.ref

object IndexedAttributeValue : ExprFactory {
    override operator fun invoke(parser: () -> Parser<Expr>): Parser<Expr> =
        inOrder(ref(parser), token("["), number, token("]"))
            .mapLeftAssoc { (expr, _, index) ->
                Expr { item ->
                    (expr.eval(item) as AttributeValue).L
                        ?.let {
                            val child = it[index.toInt()]
                            when {
                                child.L != null -> child
                                else -> AttributeValue.List(listOf(child))
                            }
                        }
                        ?: AttributeValue.Null()
                }
            }
}
