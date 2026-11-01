package dev.forkhandles.values

interface Problem {
    val constraint: String
    val message: String
    val value: Any?
}

data class ConstraintProblem(
    override val constraint: String,
    override val message: String,
    override val value: Any?
) : Problem

data class Problems(
    val causes: List<Problem>
) : Problem {
    override val constraint: String = "Problems"
    override val message: String = causes.joinToString("; ") { it.message }
    override val value: Any = causes
}

fun Problem.causes(): List<Problem> = when (this) {
    is Problems -> causes
    else -> listOf(this)
}
