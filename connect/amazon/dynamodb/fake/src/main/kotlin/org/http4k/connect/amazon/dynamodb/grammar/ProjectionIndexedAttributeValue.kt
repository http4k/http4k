package org.http4k.connect.amazon.dynamodb.grammar

import org.http4k.connect.amazon.dynamodb.model.AttributeValue
import parser4k.Parser
import parser4k.commonparsers.Tokens.number
import parser4k.commonparsers.token
import parser4k.inOrder
import parser4k.mapLeftAssoc
import parser4k.ref

@Suppress("UNCHECKED_CAST")
object ProjectionIndexedAttributeValue : ExprFactory {
    override operator fun invoke(parser: () -> Parser<Expr>): Parser<Expr> =
        inOrder(ref(parser), token("["), number, token("]"))
            .mapLeftAssoc { (expr, _, index) ->
                Expr { item ->
                    (expr.eval(item) as List<AttributeNameValue>)
                        .map { it.first to filterInward(it, index.toInt()) }
                }
            }

    private fun filterInward(l: AttributeNameValue, index: Int): AttributeValue = when {
        l.second.L == null -> l.second
        l.second.L!!.size > 1 -> AttributeValue.List(listOf(l.second.L!![index]))
        else -> AttributeValue.List(listOf(filterInward(l.first to l.second.L!![0], index)))
    }
}
