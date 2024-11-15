package org.http4k.connect.amazon.dynamodb.grammar

import org.http4k.connect.amazon.dynamodb.model.AttributeName
import org.http4k.connect.amazon.dynamodb.model.AttributeValue
import org.http4k.connect.amazon.dynamodb.model.Item
import parser4k.OutputCache
import parser4k.Parser
import parser4k.commonparsers.Tokens
import parser4k.commonparsers.token
import parser4k.inOrder
import parser4k.map
import parser4k.oneOf
import parser4k.oneOfWithPrecedence
import parser4k.oneOrMore
import parser4k.optional
import parser4k.parseWith
import parser4k.ref
import parser4k.reset
import parser4k.skipFirst
import parser4k.with

object DynamoDbUpdateGrammar {
    private val cache = OutputCache<Expr>()

    fun parse(expression: String): Expr = expression.parseWith(expr)

    private val expr = oneOrMore(
        oneOf(
            Set.with(cache),
            Remove.with(cache),
            Add.with(cache),
            Delete.with(cache)
        )
    ).map { updates ->
        Expr { item ->
            updates.fold(item) { curItem, expr ->
                @Suppress("UNCHECKED_CAST")
                curItem.copy(item = expr.eval(curItem) as Item)
            }.item
        }
    }.reset(cache)
}

// Expressions

private val Name = oneOfWithPrecedence(
    inOrder(oneOf('#'), Tokens.identifier).skipFirst().map { ref ->
        Expr { item ->
            item.names["#$ref"] ?: error("missing name $ref")
        }
    },
    Tokens.identifier.map { name -> Expr { AttributeName.of(name) } }
)

private val RawValue = oneOf(
    // item value
    Tokens.identifier.map { name ->
        Expr { item ->
            item.item[AttributeName.of(name)] ?: error("missing item value $name")
        }
    },
    // named item value
    inOrder(oneOf('#'), Tokens.identifier).skipFirst().map { ref ->
        Expr { item ->
            val name = item.names["#$ref"] ?: error("missing name $ref")
            item.item[name] ?: error("missing item value $name")
        }
    },
    // named value
    inOrder(oneOf(':'), Tokens.identifier).skipFirst().map { name ->
        Expr { item ->
            item.values[":$name"] ?: error("missing value $name")
        }
    }
)

private val Operand = oneOf(
    ref { ListAppend },
    ref { IfNotExists },
    RawValue
)

private val Value = oneOfWithPrecedence(
    inOrder(
        Operand,
        token("+"),
        Operand
    ).map { (op1, _, op2) ->
        Expr { item ->
            (op1.eval(item) as AttributeValue) + (op2.eval(item) as AttributeValue)
        }
    },
    inOrder(
        Operand,
        token("-"),
        Operand
    ).map { (op1, _, op2) ->
        Expr { item ->
            val val1 = op1.eval(item) as AttributeValue
            val val2 = op2.eval(item) as AttributeValue
            val1 - val2
        }
    },
    Operand
)

private val NameValuePair = inOrder(
    optional(token(",")),
    Name,
    Tokens.whitespace,
    RawValue
).skipFirst()

private val index = inOrder(
    token("["),
    Tokens.number,
    token("]")
).map { it.second }

// Functions

private val ListAppend: Parser<Expr> = inOrder(
    token("list_append("),
    Value,
    token(","),
    Value,
    token(")")
).skipFirst().map { (valExpr1, _, valExpr2) ->
    Expr { item ->
        valExpr1.eval(item) as AttributeValue + valExpr2.eval(item) as AttributeValue
    }
}

private val IfNotExists: Parser<Expr> = inOrder(
    token("if_not_exists("),
    Name,
    token(","),
    Value,
    token(")")
).skipFirst().map { (nameExp, _, valueExp) ->
    Expr { item ->
        val name = nameExp.eval(item) as AttributeName
        val value = valueExp.eval(item) as AttributeValue
        item.item[name] ?: value
    }
}

// Actions

private val Remove = inOrder(
    oneOf(token("REMOVE"), token("remove")), // fixme possible to be case insensitive?
    oneOrMore(
        inOrder(
            optional(Tokens.whitespace),
            Name,
            optional(index)
        ).skipFirst()
    )
).skipFirst().map { names ->
    Expr { item ->
        names.fold(item) { curItem, nameAndIndex ->
            val name = nameAndIndex.first.eval(curItem) as AttributeName
            val updated = nameAndIndex.second?.toInt()
                ?.let { index -> curItem.item + (name to curItem.item[name]!!.delete(index)) } // remove element from list
                ?: (curItem.item - name)  // remove attribute

            curItem.copy(item = updated)
        }.item
    }
}

private val Set = inOrder(
    oneOf(token("SET"), token("set")), // fixme possible to be case insensitive?
    oneOrMore(
        inOrder(
            optional(token(",")),
            Name,
            optional(index),
            token("="),
            Value
        ).skipFirst()
    )
).skipFirst().map { equations ->
    Expr { item ->
        item.item + equations.associate { (nameExp, indexStr, _, valueExp) ->
            val name = nameExp.eval(item) as AttributeName
            val value = (valueExp.eval(item) as AttributeValue)
            val toSet = indexStr?.toInt()
                ?.let { item.item[name]!!.with(it, value) } // set element of list
                ?: value // set entire attribute
            name to toSet
        }
    }
}

private val Add = inOrder(
    oneOf(token("ADD"), token("add")), // fixme possible to be case insensitive?
    oneOrMore(NameValuePair)
).skipFirst().map { adds ->
    Expr { item ->
        item.item + adds.map { (nameExp, _, valueExp) ->
            val name = nameExp.eval(item) as AttributeName
            val value = valueExp.eval(item) as AttributeValue
            name to (item.item[name]?.plus(value) ?: value)

        }
    }
}

private val Delete = inOrder(
    oneOf(token("DELETE"), token("delete")), // fixme possible to be case insensitive?
    oneOrMore(NameValuePair)
).skipFirst().map { adds ->
    Expr { item ->
        item.item + adds.mapNotNull { (nameExp, _, valueExp) ->
            val name = nameExp.eval(item) as AttributeName
            item.item[name]?.let { existing ->
                val add = valueExp.eval(item) as AttributeValue
                name to (existing - add)
            }
        }
    }
}
