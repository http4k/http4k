package org.http4k.connect.amazon.dynamodb.grammar

import parser4k.Parser
import parser4k.ref

object And : ExprFactory {
    override operator fun invoke(parser: () -> Parser<Expr>): Parser<Expr> =
        binaryExpr(ref(parser), "AND", ::And)
}

fun And(left: Expr, right: Expr) = Expr { item ->
    (left.eval(item) as Boolean) && (right.eval(item) as Boolean)
}
