package org.http4k.connect.amazon.dynamodb.grammar

import parser4k.Parser
import parser4k.ref

object Not : ExprFactory {
    override operator fun invoke(parser: () -> Parser<Expr>) =
        unaryExpr(ref(parser), "NOT") { expr ->
            object : Expr {
                override fun eval(item: ItemWithSubstitutions) = !(expr.eval(item) as Boolean)

                // pass the validation walk on, so a nested operand is still checked - see Expr.validate
                override fun validate(item: ItemWithSubstitutions) = item.validateAll(expr)
            }
        }
}
