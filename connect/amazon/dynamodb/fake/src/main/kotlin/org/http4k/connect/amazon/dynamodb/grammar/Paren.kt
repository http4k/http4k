package org.http4k.connect.amazon.dynamodb.grammar

import parser4k.Parser
import parser4k.commonparsers.token
import parser4k.inOrder
import parser4k.ref
import parser4k.skipWrapper

object Paren : ExprFactory {
    override operator fun invoke(parser: () -> Parser<Expr>) =
        inOrder(token("("), ref(parser), token(")")).skipWrapper()
}
