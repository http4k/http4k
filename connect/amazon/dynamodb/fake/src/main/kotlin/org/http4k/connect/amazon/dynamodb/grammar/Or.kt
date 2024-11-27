package org.http4k.connect.amazon.dynamodb.grammar

import parser4k.Parser
import parser4k.ref

object Or : ExprFactory {
    override operator fun invoke(parser: () -> Parser<Expr>): Parser<Expr> =
        binaryExpr(ref(parser), "OR") { left, right ->
            Expr {
                (left.eval(it) as Boolean) || (right.eval(it) as Boolean)
            }
        }
}
