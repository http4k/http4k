package org.http4k.connect.amazon.dynamodb.grammar

import org.http4k.connect.amazon.dynamodb.model.AttributeValue
import parser4k.Parser
import parser4k.commonparsers.token
import parser4k.inOrder
import parser4k.map
import parser4k.ref
import parser4k.skipWrapper

object AttributeExists : ExprFactory {
    override operator fun invoke(parser: () -> Parser<Expr>) =
        inOrder(token("attribute_exists"), token("("), ref(parser), token(")"))
            .skipWrapper()
            .map { (_, attr) ->
                Expr {
                    (attr.eval(it) as AttributeValue).NULL == null
                }
            }
}
