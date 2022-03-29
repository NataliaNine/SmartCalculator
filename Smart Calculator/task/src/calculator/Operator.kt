package calculator

import kotlin.math.pow

class Operator(rawValue: String) : Element(rawValue) {
    private val regexAllMinus = Regex("-+")
    private val regexAllPlus = Regex("\\++")

    override fun toString(): String {
        return " $rawValue "
    }

    fun toCleanString(): String {
        return when (rawValue[0]) {
            '+' -> "+"
            '-' ->
                if (rawValue.matches(regexAllMinus) && rawValue.length % 2 == 1)
                    "-"
                else
                    "+"
            '*' -> "*"
            '/' -> "/"
            '^' -> "^"
            else -> throw Exception("Invalid Operator: $rawValue")
        }
    }

    fun getMathFunction(): (Int, Int) -> Int {
        when (rawValue[0]) {
            '+' -> return getActualPlusOrMinusOperator()
            '-' -> return getActualPlusOrMinusOperator()
            '*' -> return Math::multiplyExact
            '/' -> return Math::floorDiv
            '^' -> return {x, y -> x.toDouble().pow(y.toDouble()).toInt()}
            else -> throw Exception("Invalid Operator: $rawValue")
        }
    }

    private fun getActualPlusOrMinusOperator(): (x: Int, y: Int) -> Int {
        val isAllMinus = rawValue.matches(regexAllMinus)
        val isAllPlus = rawValue.matches(regexAllPlus)

        return when {
            isAllPlus -> Math::addExact
            isAllMinus && rawValue.length % 2 == 1 -> Math::subtractExact
            isAllMinus && rawValue.length % 2 == 0 -> Math::addExact
            else -> throw Exception("Invalid Operator: $rawValue")
        }
    }



    fun precedence(): Int {
        val isAllMinus = rawValue.matches(regexAllMinus)
        val isAllPlus = rawValue.matches(regexAllPlus)

        // PEMDAS - () is 0, ^ is 1, * is 2, / is 3, + is 4, - is 5
        return when {
            rawValue == "^" -> 1
            rawValue == "*" -> 2
            rawValue == "/" -> 3
            isAllPlus || (isAllMinus && rawValue.length % 2 == 0) -> 4
            isAllMinus -> 4
            else -> throw Exception("Unknown precedence for $rawValue")
        }
    }
}