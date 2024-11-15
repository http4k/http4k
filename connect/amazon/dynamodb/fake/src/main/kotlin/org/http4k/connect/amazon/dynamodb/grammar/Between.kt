package org.http4k.connect.amazon.dynamodb.grammar

import parser4k.Parser
import parser4k.commonparsers.token
import parser4k.inOrder
import parser4k.map
import parser4k.ref

object Between : ExprFactory {
    override operator fun invoke(parser: () -> Parser<Expr>) =
        inOrder(ref(parser), token("BETWEEN"), ref(parser), token("AND"), ref(parser))
            .map {
                And(GreaterThanOrEqual(it.val1, it.val3), LessThanOrEqual(it.val1, it.val5))
            }
}
