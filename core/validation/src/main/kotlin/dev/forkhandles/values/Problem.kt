package dev.forkhandles.values

interface Problem {
    val constraint: String
    val message: String
    val value: String
}

data class ConstraintProblem(
    override val constraint: String,
    override val message: String,
    override val value: String
) : Problem

data class Problems(
    val causes: List<Problem>
) : Problem {
    override val constraint: String = "Problems"
    override val message: String = causes.joinToString("; ") { it.message }
    override val value: String = causes.joinToString("; ") { it.value }
}

fun Problem.causes(): List<Problem> = when (this) {
    is Problems -> causes
    else -> listOf(this)
}
