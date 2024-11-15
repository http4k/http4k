package org.http4k.connect.amazon.dynamodb.grammar

import parser4k.Parser
import parser4k.ref

object LessThanOrEqual : ExprFactory {
    override operator fun invoke(parser: () -> Parser<Expr>): Parser<Expr> =
        binaryExpr(ref(parser), "<=", ::LessThanOrEqual)
}

fun LessThanOrEqual(attr1: Expr, attr2: Expr) = Expr { item ->
    item.comparable(attr1) <= item.comparable(attr2)
}
