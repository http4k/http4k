package org.http4k.connect.amazon.dynamodb.grammar

import org.http4k.connect.amazon.dynamodb.model.AttributeValue
import parser4k.Parser
import parser4k.ref

object Size : ExprFactory {
    override operator fun invoke(parser: () -> Parser<Expr>): Parser<Expr> =
        unaryExpr(ref(parser), "size") { attr ->
            Expr { item ->
                AttributeValue.Num(
                    attr.eval(item).asString()
                        .takeIf { it != NULLMARKER }.toString().length
                )
            }
        }
}
