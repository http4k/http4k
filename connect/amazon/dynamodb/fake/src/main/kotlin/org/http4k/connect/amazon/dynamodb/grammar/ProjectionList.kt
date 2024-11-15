package org.http4k.connect.amazon.dynamodb.grammar

import parser4k.Parser
import parser4k.ref

@Suppress("UNCHECKED_CAST")
object ProjectionList : ExprFactory {
    override operator fun invoke(parser: () -> Parser<Expr>): Parser<Expr> =
        binaryExpr(ref(parser), ",") { left, right ->
            Expr {
                (left.eval(it) as List<AttributeNameValue>) + (right.eval(it) as List<AttributeNameValue>)
            }
        }
}
