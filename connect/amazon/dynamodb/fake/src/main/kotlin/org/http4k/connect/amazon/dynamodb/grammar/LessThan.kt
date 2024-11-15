package org.http4k.connect.amazon.dynamodb.grammar

import parser4k.Parser
import parser4k.ref

object LessThan : ExprFactory {
    override operator fun invoke(parser: () -> Parser<Expr>): Parser<Expr> =
        binaryExpr(ref(parser), "<") { attr1, attr2 ->
            Expr { item ->
                item.comparable(attr1) < item.comparable(attr2)
            }
        }
}
