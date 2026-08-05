package org.http4k.connect.amazon.dynamodb.grammar

import parser4k.Parser
import parser4k.ref

object And : ExprFactory {
    override operator fun invoke(parser: () -> Parser<Expr>): Parser<Expr> =
        binaryExpr(ref(parser), "AND", ::And)
}

fun And(left: Expr, right: Expr): Expr = object : Expr {
    override fun eval(item: ItemWithSubstitutions) =
        (left.eval(item) as Boolean) && (right.eval(item) as Boolean)

    // eval short-circuits, so validation of the right operand cannot ride on it - see Expr.validate
    override fun validate(item: ItemWithSubstitutions) = item.validateAll(left, right)
}
