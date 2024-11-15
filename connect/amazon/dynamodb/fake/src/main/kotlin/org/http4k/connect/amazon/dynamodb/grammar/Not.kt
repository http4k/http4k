package org.http4k.connect.amazon.dynamodb.grammar

import parser4k.Parser
import parser4k.ref

object Not : ExprFactory {
    override operator fun invoke(parser: () -> Parser<Expr>) =
        unaryExpr(ref(parser), "NOT") { expr ->
            Expr { !(expr.eval(it) as Boolean) }
        }
}
